package Splitwise;

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

    public String getUser(){
        return user.getName();
    }

    public void setAmount(double amount){
        this.amount = amount;
    }
}

// Service class
abstract class Expense {

}

// Orchestrator class
class ExpenseManager {

}

// Driver : Main class
public class Splitwise {
    public static void main(String args[]){

    }
}
