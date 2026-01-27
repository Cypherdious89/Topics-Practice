package Splitwise;
import java.util.*;

enum ExpenseType {
    EQUAL, EXACT, PERCENT
}
// Model class
class User {
    private String id;
    private String name;

    User(String id, String name){
        this.id = id;
        this.name = name;
    }

    public String getName(){
        return this.name;
    }

    public String getId(){
        return this.id;
    }
}

// Model class
abstract class Split {
    private User user;
    protected double amount;

    Split(User user){
        this.user = user;
    }

    public double getAmount(){
        return amount;
    }

    public User getUser(){
        return user;
    }

    public void setAmount(double amount){
        this.amount = amount;
    }
}

class EqualSplit extends Split {
    public EqualSplit(User user){
        super(user);
    }
}

class ExactSplit extends Split {
    public ExactSplit(User user, double amount){
        super(user);
        this.amount = amount;
    }
}

class PercentageSplit extends Split {
    double percent;
    public PercentageSplit(User user, double percent){
        super(user);
        this.percent = percent;
    }
}

// Service class
abstract class Expense {
    private double amount;
    private User paidBy;
    private List<Split> splits;

    public Expense(double amount, User paidBy, List<Split> splits){
        this.amount = amount;
        this.paidBy = paidBy;
        this.splits = splits;
    }

    public double getAmount(){
        return amount;
    }

    public User getPaidBy(){
        return paidBy;
    }

    public List<Split> getSplits(){
        return splits;
    }

    public abstract boolean validate();
}

class EqualExpense extends Expense {
    public EqualExpense(double amount, User paidBy, List<Split> splits){
        super(amount, paidBy, splits);
    }

    @Override 
    public boolean validate(){
        return true;
    }
}

class ExactExpense extends Expense {
    public ExactExpense(double amount, User paidBy, List<Split> splits){
        super(amount, paidBy, splits);
    }

    @Override
    public boolean validate(){
        double totalAmount = getAmount();
        double sumSplitAmount = 0;
        for(Split split: getSplits()){
            sumSplitAmount += split.getAmount();
        }
        return (totalAmount - sumSplitAmount) < 0.01;
    }
}

class PercentageExpense extends Expense {
    public PercentageExpense(double amount, User paidBy, List<Split> splits){
        super(amount, paidBy, splits);
    }

    @Override
    public boolean validate(){
        double totalPercentage = 0.0;
        for(Split split: getSplits()){
            totalPercentage += ((PercentageSplit) split).percent;
        }
        return (100 - totalPercentage) < 0.01;
    }
}

// Orchestrator class
class ExpenseManager {
    Map<String, User> userMap = new HashMap<>();
    Map<String, Map<String, Double>> balanceSheet = new HashMap<>();

    public void addUser(User user){
        userMap.put(user.getId(), user);
        balanceSheet.put(user.getId(), new HashMap<>());
    }

    public void addExpense(ExpenseType type, double amount, String paidById, List<Split> splits){
        Expense expense = createExpense(type, amount, paidById, splits);

        



        if(expense != null && expense.validate()){
            for(Split split : splits){
                String paidToId = split.getUser().getId();
                if(paidById.equals(paidToId))   continue;

                //Update paidBy's sheet: They are owed money(+)
                Map<String, Double> balances = balanceSheet.get(paidById);
                balances.put(paidToId, balances.getOrDefault(paidToId, 0.0) + split.getAmount());

                //Update paidTo's sheet: They owe money(-)
                balances = balanceSheet.get(paidToId);
                balances.put(paidById, balances.getOrDefault(paidById, 0.0) - split.getAmount());
            }
        }
    }

    private Expense createExpense(ExpenseType type, double amount, String paidById, List<Split> splits) {
        User paidBy = userMap.get(paidById);

        switch (type) {
            case EQUAL:
                double splitAmount = ((double) Math.round(amount * 100.0 / splits.size())) / 100.0;
                for (Split split : splits) {
                    split.setAmount(splitAmount);
                }
                splits.get(0).setAmount(splitAmount + (amount - splitAmount * splits.size()));
                return new EqualExpense(amount, paidBy, splits);
            case EXACT:
                return new ExactExpense(amount, paidBy, splits);
            case PERCENT:
                for (Split split : splits) {
                    PercentageSplit percentSplit = (PercentageSplit) split;
                    split.setAmount((amount * percentSplit.percent) / 100);
                }
                return new PercentageExpense(amount, paidBy, splits);
            default:
                return null;
        }
    }

    public void showBalance(String userId) {
        boolean hasBalances = false;
        for (Map.Entry<String, Double> entry : balanceSheet.get(userId).entrySet()) {
            if (entry.getValue() > 0) {
                hasBalances = true;
                printBalance(userId, entry.getKey(), entry.getValue());
            }
        }

        if (!hasBalances) {
            System.out.println("No balances for " + userId);
        }
    }

    public void showAllBalances(){
        boolean hasBalances = false;
        for(String userId: balanceSheet.keySet()){
            for (Map.Entry<String, Double> entry : balanceSheet.get(userId).entrySet()) {
                if (entry.getValue() > 0) {
                    hasBalances = true;
                    printBalance(userId, entry.getKey(), entry.getValue());
                }
            }
        }
        if(!hasBalances){
            System.out.println("No balances");
        }
    }

    private void printBalance(String user1, String user2, double amount){
        String name1 = userMap.get(user1).getName();
        String name2 = userMap.get(user2).getName();

        if (amount < 0) {
            System.out.println(name1 + " owes " + name2 + " : " + Math.abs(amount));
        } else {
            System.out.println(name2 + " owes " + name1 + " : " + amount);
        }
    }
}

// Driver : Main class
public class Splitwise {
    public static void main(String args[]){
        ExpenseManager manager = new ExpenseManager();

        manager.addUser(new User("u1", "User1"));
        manager.addUser(new User("u2", "User2"));
        manager.addUser(new User("u3", "User3"));
        manager.addUser(new User("u4", "User4"));

        //Equal Expense
        List<Split> splits1 = Arrays.asList(
            new EqualSplit(manager.userMap.get("u1")),
            new EqualSplit(manager.userMap.get("u2")),
            new EqualSplit(manager.userMap.get("u3")),
            new EqualSplit(manager.userMap.get("u4"))
        );
        
        manager.addExpense(ExpenseType.EQUAL, 1000, "u1", splits1);

        System.out.println(" --- After Equal Expense ---");
        manager.showAllBalances();

        //Exact Expense
        List<Split> splits2 = Arrays.asList(
            new ExactSplit(manager.userMap.get("u2"), 300.0),
            new ExactSplit(manager.userMap.get("u3"), 400.0),
            new ExactSplit(manager.userMap.get("u4"), 300.0)
        );

        manager.addExpense(ExpenseType.EXACT, 1000, "u1", splits2);

        System.out.println(" --- After Exact Expense ---");
        manager.showAllBalances();

        //Percent Expense
        List<Split> split3 = Arrays.asList(
            new PercentageSplit(manager.userMap.get("u1"), 40),
            new PercentageSplit(manager.userMap.get("u2"), 25),
            new PercentageSplit(manager.userMap.get("u3"), 25),
            new PercentageSplit(manager.userMap.get("u4"), 10)
        );

        manager.addExpense(ExpenseType.PERCENT, 1500, "u4", split3);

        System.out.println(" --- After Percentage Expense ---");
        manager.showAllBalances();

    }
}
