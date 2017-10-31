import hlt.*;

import java.io.IOException;

import java.nio.file.Files;

import java.nio.file.OpenOption;
import java.nio.file.Paths;

import java.nio.file.StandardOpenOption;

import java.time.Duration;
import java.time.LocalDate;

import nn.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBot {

    private static final int MAX_LOAD_PERCENTAGE = 70;
    private static final int MAX_DOCKED_SHIPS_PERCENTAGE = 90;
    private static int myPlayerId;

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Fitibaldi");

        final ArrayList<Move> moveList = new ArrayList<>();
        List<NNParametersHalite> nnList = new ArrayList<NNParametersHalite>();
        for (;;) {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());
            myPlayerId = gameMap.getMyPlayerId();

            Map<Planet, Integer> dockingPlan = new HashMap<Planet, Integer>();

            boolean nnSaved = false;
            try {
                for (final Ship ship : gameMap.getMyPlayer()
                                              .getShips()
                                              .values()) {
                    if (args.length > 0 && args[0].equals("NN")) {
                        NNParametersHalite nn = new NNParametersHalite();
                        nn.calcNNValues(ship, gameMap, myPlayerId);

                        if (!nnSaved) {
                            for (NNParametersHalite analyze : nnList) {
                                String tResult = "0\n";
                                if (nn.getMyRang() >= analyze.getMyRang()) {
                                    tResult = "1\n";
                                } else {
                                    tResult = "0\n";
                                }
                                try {
                                    Files.write(Paths.get("data/t.txt"), tResult.getBytes(), StandardOpenOption.APPEND);
                                } catch (IOException e) {
                                }
                            }
                            nnList.clear();
                            nnSaved = true;
                        }

                        nnList.add(nn);
                        try {
                            Files.write(Paths.get("data/x.txt"), nn.toString().getBytes(), StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            System.out.println("error");
                        }

                    }

                    boolean movementChoosen = false;
                    //Check docked
                    if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                        continue;
                    }

                    Map<Double, Entity> nearestObjects = gameMap.nearbyEntitiesByDistance(ship);
                    int myDockedShips = 0;
                    for (Ship countShip : gameMap.getMyPlayer()
                                                 .getShips()
                                                 .values()) {
                        if (countShip.getDockingStatus() == Ship.DockingStatus.Docked ||
                            countShip.getDockingStatus() == Ship.DockingStatus.Docking) {
                            myDockedShips++;
                        }
                    }

                    if ((myDockedShips / gameMap.getMyPlayer()
                                                .getShips()
                                                .size()) * 100 <= MAX_DOCKED_SHIPS_PERCENTAGE) {
                        //Find where to dock
                        for (Map.Entry<Double, Entity> obj : nearestObjects.entrySet()) {
                            if (!movementChoosen && obj.getValue() instanceof Planet) {
                                Planet planet = (Planet) obj.getValue();

                                if (willDock(planet, dockingPlan)) {
                                    if (ship.canDock(planet)) {
                                        moveList.add(new DockMove(ship, planet));
                                        if (dockingPlan.get(planet) != null) {
                                            dockingPlan.put(planet, dockingPlan.get(planet) + 1);
                                        } else {
                                            dockingPlan.put(planet, 0);
                                        }
                                        movementChoosen = true;
                                        break;
                                    } else {
                                        final ThrustMove newThrustMove =
                                            Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                                        moveList.add(newThrustMove);
                                        if (dockingPlan.get(planet) != null) {
                                            dockingPlan.put(planet, dockingPlan.get(planet) + 1);
                                        } else {
                                            dockingPlan.put(planet, 1);
                                        }
                                        movementChoosen = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    if (movementChoosen) {
                        continue;
                    }

                    //Find where to attack
                    for (Map.Entry<Double, Entity> obj : nearestObjects.entrySet()) {
                        Entity enemy = obj.getValue();

                        if (enemy instanceof Ship) {
                            if (!movementChoosen && enemy.getOwner() != myPlayerId) {
                                final ThrustMove newAttackMove =
                                    Navigation.navigateShipTowardsTargetNoCollision(gameMap, ship, enemy,
                                                                                    Constants.MAX_SPEED, true, 45);
                                moveList.add(newAttackMove);
                                movementChoosen = true;
                                break;
                            }
                        }
                    }
                }

                Networking.sendMoves(moveList);

                if (args.length > 0 && args[0].equals("LOG")) {
                    try {
                        Files.write(Paths.get("data/errors.log"), "INFO\n".getBytes(), StandardOpenOption.APPEND);
                    } catch (IOException f) {
                    }
                    for (Move move : moveList) {
                        StringBuilder moveString = new StringBuilder();
                        moveString.append(move.getShip())
                                  .append(",")
                                  .append(move.getType())
                                  .append(",")
                                  .append("\n");
                        try {
                            Files.write(Paths.get("data/errors.log"), moveString.toString().getBytes(),
                                        StandardOpenOption.APPEND);
                        } catch (IOException f) {
                        }
                    }
                }

            } catch (Exception e) {
                if (args.length > 0 && args[0].equals("LOG")) {
                    try {
                        Files.write(Paths.get("data/errors.log"), ("ERROR " + moveList.size() + "\n").getBytes(),
                                    StandardOpenOption.APPEND);
                    } catch (IOException f) {
                    }
                    for (Move move : moveList) {
                        StringBuilder moveString = new StringBuilder();
                        moveString.append(move.getShip())
                                  .append(",")
                                  .append(move.getType())
                                  .append(",")
                                  .append("\n");
                        try {
                            Files.write(Paths.get("data/errors.log"), moveString.toString().getBytes(),
                                        StandardOpenOption.APPEND);
                        } catch (IOException f) {
                        }
                    }
                }
            }
        }
    }

    private static boolean willDock(Planet planet, Map<Planet, Integer> dockingPlan) {
        if (planet.isFull() || (planet.isOwned() && planet.getOwner() != myPlayerId) ||
            ((planet.getDockedShips().size() / planet.getDockingSpots()) * 100 >= MAX_LOAD_PERCENTAGE)) {
            return false;
        }

        if (dockingPlan.get(planet) != null && dockingPlan.get(planet) >= (planet.getDockingSpots() - planet.getDockedShips().size())) {
            return false;
        }

        return true;
    }
}
