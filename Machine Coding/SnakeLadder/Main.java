import java.util.*;

// Model class
class Jump {
    private final int start;
    private final int end;

    Jump(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart(){
        return start;
    
    }

    public int getEnd(){
        return end;
    }

    public boolean isSnake(){
        return start > end;
    }
}

// Model class
class Player {
    private final String id;
    private final String name;
    private int position;

    Player(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.position = 0;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}

// Service class
class Dice {
    private final int numberOfDice;
    private final Random random;

    public Dice(int numberOfDice) {
        this.numberOfDice = numberOfDice;
        this.random = new Random();
    }

    public int roll() { 
        int total = 0;
        for (int i = 0; i < numberOfDice; i++) {
            total += random.nextInt(6) + 1;
        }
        return total;
    }
}

// Model class
class Board {
    private final int size;
    private final Map<Integer, Jump> jumps;
    private final Set<Integer> jumpEnds;

    Board (int size){
        this.size = size;
        this.jumps = new HashMap<>();
        this.jumpEnds = new HashSet<>();
    }

    public void addJump(int start, int end){
        if(jumps.containsKey(start)){
            throw new IllegalArgumentException(
                "Jump already exists at position " + start + "." 
            );
        }

        if(jumpEnds.contains(start) || jumpEnds.contains(end)){
            throw new IllegalArgumentException(
                "Invalid jump: start/end conflicts with existing jump"
            );
        }

        jumps.put(start, new Jump(start, end));
        jumpEnds.add(end);
    }

    public int getNewPositionAfterJump(int currentPosition){
        if(jumps.containsKey(currentPosition)){
            Jump jump = jumps.get(currentPosition);
            String type = jump.isSnake() ? "Snake" : "Ladder";
            System.out.println(" -- Logic:" + type + " found at " + currentPosition + ", Moving to -> " + jump.getEnd());
            return jump.getEnd();
        }
        return currentPosition;
    }

    public int getSize(){
        return this.size;
    }
}

// Orchestrator class
class Game {
    private final Board board;
    private final Queue<Player> players;
    private final Dice dice;
    private final List<Player> winners;

    public Game(int boardSize, List<Player> playerList){
        this.board = new Board(boardSize);
        this.players = new ArrayDeque<>(playerList);
        this.dice = new Dice(1);
        this.winners = new ArrayList<>();
    }

    public void addSnake(int start, int end){
        if(end >= start) {
            throw new IllegalArgumentException(
                "Invalid snake: end position must be less than start position"
            );
        }
        board.addJump(start, end);
    }

    public void addLadder(int start, int end){
        if(start >= end){
            throw new IllegalArgumentException(
                "Invalid ladder: start position must be less than end position"
            );
        }
        board.addJump(start, end);
    }

    public void printResults(){
        System.out.println("--- FINAL RANKINGS ---");
        for(int i=0; i<winners.size(); i++){
            System.out.println((i+1) + " - " + winners.get(i).getName());
        }
    }

    public void play(){
        if (players.isEmpty()) {
            throw new IllegalStateException("No players in the game");
        }
        System.out.println("--- GAME STARTED ---");

        while(!players.isEmpty()){
            Player currentPlayer = players.poll();
            int rollValue = dice.roll();

            int currentPosition = currentPlayer.getPosition();
            int nextPosition = currentPosition + rollValue;

            System.out.println(currentPlayer.getName() + " rolled a " + rollValue + ".");

            if(nextPosition > board.getSize()){
                System.out.println("Move ignored. Exceeds (" + board.getSize() + ")");
                players.offer(currentPlayer);
            } else {
                int newPosition = board.getNewPositionAfterJump(nextPosition);
                currentPlayer.setPosition(newPosition);
                System.out.println(currentPlayer.getName() + " moves to : " + newPosition);

                if(newPosition == board.getSize()){
                    winners.add(currentPlayer);
                    System.out.println("**** " + currentPlayer.getName() + " HAS WON. Finished at Rank " + winners.size() + ". ****");
                } else {
                    players.offer(currentPlayer);
                }
            }
        }
        System.out.println(" --- GAME OVER ---");
        printResults();
    }
}


// Driver : Main class
public class Main {
    public static void main(String args[]){
        Player p1 = new Player("Adam");
        Player p2 = new Player("Blake");
        Player p3 = new Player("Charles");
        Player p4 = new Player("Dave");

        Game game = new Game(100, Arrays.asList(p1, p2, p3, p4));

        //setup snakes
        game.addSnake(95, 11);
        game.addSnake(88, 69);
        game.addSnake(75, 65);
        game.addSnake(64, 40);
        game.addSnake(55, 22);
        game.addSnake(49, 18);
        game.addSnake(30, 12);
        game.addSnake(19, 5);

        //setup ladders
        game.addLadder(6, 15);
        game.addLadder(10, 88);
        game.addLadder(25, 66);
        game.addLadder(44, 78);
        game.addLadder(72, 98);

        game.play();
    }
}