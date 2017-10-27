import hlt.*;

import java.util.ArrayList;
import java.util.Map;

public class MyBot2 {

    private static final int MIN_PLANET_PRODUCTION = 0;
    private static int myPlayerId;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Fitibaldi.vol2");

        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());
            myPlayerId = gameMap.getMyPlayerId();

            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                boolean movemenetChoosen = false;
                //Check docked
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }
                
                //Find where to dock
                Map<Double, Entity> nearestObjects = gameMap.nearbyEntitiesByDistance(ship);
                for (Map.Entry<Double, Entity> obj : nearestObjects.entrySet()) {
                    if (obj.getValue() instanceof Planet) {
                        Planet planet = (Planet) obj.getValue();
                        
                        if (!willDock(planet)) {
                            continue;
                        }
                        
                        if (ship.canDock(planet)) {
                            moveList.add(new DockMove(ship, planet));
                            movemenetChoosen = true;
                            break;
                        } else {
                            final ThrustMove newThrustMove =
                                Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                            if (newThrustMove != null && newThrustMove.getType() != null) {
                                moveList.add(newThrustMove);
                                movemenetChoosen = true;
                                break;
                            }
                        }
                    }
                }
                
                if (movemenetChoosen) {
                    continue;
                }
                
                //Find where to attack
                for (Map.Entry<Double, Entity> obj : nearestObjects.entrySet()) {
                    if (obj.getValue() != null) {
                        Entity enemy = obj.getValue();

                        if (enemy.getOwner() != myPlayerId) {
                            final ThrustMove newAttackMove =
                                Navigation.navigateShipTowardsTarget(gameMap, ship, enemy, Constants.MAX_SPEED, true, 45, 15);
                            moveList.add(newAttackMove);
                            movemenetChoosen = true;
                            break;
                        }
                    }
                }
            }
            Networking.sendMoves(moveList);
        }
    }

    private static boolean willDock(Planet planet) {
        if (planet.isFull() 
            || (planet.isOwned() && planet.getOwner() != myPlayerId)) {
            return false;
        }
        return true;
    }
}
