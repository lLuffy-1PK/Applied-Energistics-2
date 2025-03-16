package appeng.me.pathfinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import appeng.me.cache.PathGridCache;

public class BackbonePathSegment extends PathSegment {

    IPathItem startNode;
    private Set<IPathItem> controllerRoutes;
    private Map<BackbonePathSegment, List<IPathItem>> neighbours;

    public BackbonePathSegment(IPathItem node, final PathGridCache myPGC, final Set<IPathItem> semiOpen, final Set<IPathItem> closed) {
        super(myPGC, new ArrayList<>(), semiOpen, closed);
        startNode = node;
    }

    private Set<IPathItem> getControllerRoutes() {
        if (controllerRoutes == null) {
            controllerRoutes = new HashSet<>();
        }
        return controllerRoutes;
    }

    private Map<BackbonePathSegment, List<IPathItem>> getNeighbours() {
        if (neighbours == null) {
            neighbours = new HashMap<>();
        }
        return neighbours;
    }

    void addControllerRoute(IPathItem pi) {
        getControllerRoutes().add(pi);
    }

    void addPathToNeighbour(BackbonePathSegment nb, IPathItem connection) {
        List<IPathItem> path = new ArrayList<>();
        IPathItem pi = connection;
        while (pi != null && pi != nb.startNode) {
            path.add(0, pi);
            pi = pi.getControllerRoute();
        }
        getNeighbours().put(nb, path);
    }

    public void selectControllerRoute() {
        if (!getControllerRoutes().isEmpty()) {
            startNode.setControllerRoute(getControllerRoutes().iterator().next(), false);
        }
    }

    public boolean switchControllerRoute() {
        if (getControllerRoutes().isEmpty() || startNode.getControllerRoute() == null) return false;
        if (startNode.getControllerRoute().getControllerRoute() != null
                && startNode.getControllerRoute().getControllerRoute().canSupportMoreChannels()) {
            return true;
        }
        getControllerRoutes().remove(startNode.getControllerRoute());
        if (getControllerRoutes().isEmpty()) return false;
        startNode.setControllerRoute(getControllerRoutes().iterator().next(), false);
        return true;
    }

    void removeNeighbour(BackbonePathSegment bs) {
        List<IPathItem> pathToRemoved = getNeighbours().get(bs);
        if (pathToRemoved == null) return; // Нет пути для удаления

        for (Map.Entry<BackbonePathSegment, List<IPathItem>> entry : bs.getNeighbours().entrySet()) {
            BackbonePathSegment nb = entry.getKey();
            if (nb == this || getNeighbours().containsKey(nb)) continue;
            List<IPathItem> path = new ArrayList<>(entry.getValue());
            path.addAll(0, pathToRemoved);
            getNeighbours().put(nb, path);
        }

        getNeighbours().remove(bs);
    }

    public void transferToNeighbours() {
        if (getNeighbours().isEmpty()) return;
        for (BackbonePathSegment nb : new HashSet<>(getNeighbours().keySet())) {
            nb.removeNeighbour(this);
        }
        BackbonePathSegment nb = getNeighbours().keySet().iterator().next();
        List<IPathItem> path = getNeighbours().get(nb);
        IPathItem controller = nb.startNode;
        for (IPathItem pi : path) {
            pi.setControllerRoute(controller, false);
            controller = pi;
        }
        startNode.setControllerRoute(controller, false);
    }

    private void reset() {
        open.add(startNode);
        closed.add(startNode);
        closed.addAll(getControllerRoutes());
    }

    public static void reset(Map<IPathItem, BackbonePathSegment> backbone) {
        if (backbone.isEmpty()) return;
        backbone.values().iterator().next().closed.clear();
        for (BackbonePathSegment bs : backbone.values()) {
            bs.reset();
        }
    }

    public boolean isValid() {
        return controllerRoutes != null && !controllerRoutes.isEmpty();
    }
}
