package com.example.emanager.models;

public class PersonExpense {
    private String personName; // Name of the person
    private double amount; // Amount of the expense
    private String groupId; // ID of the group
    private String groupName; // Name of the group
    private String expenseName; // Name of the expense (e.g., "Dinner")
    private String expenseId; // Firestore document ID for this expense

    // Constructor with all fields
    public PersonExpense(String personName, double amount, String groupId, String groupName, String expenseName, String expenseId) {
        this.personName = personName;
        this.amount = amount;
        this.groupId = groupId;
        this.groupName = groupName;
        this.expenseName = expenseName;
        this.expenseId = expenseId;
    }

    // Constructor without expenseId (optional)
    public PersonExpense(String personName, double amount, String groupId, String groupName, String expenseName) {
        this.personName = personName;
        this.amount = amount;
        this.groupId = groupId;
        this.groupName = groupName;
        this.expenseName = expenseName;
    }

    // Constructor with 4 parameters
    public PersonExpense(String personName, double amount, String groupId, String groupName) {
        this.personName = personName;
        this.amount = amount;
        this.groupId = groupId;
        this.groupName = groupName;
        // Leave expenseName and expenseId as null
    }


    // Default constructor (required for Firestore deserialization)
    public PersonExpense() {}

    // Getter and Setter for personName
    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    // Getter and Setter for amount
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    // Getter and Setter for groupId
    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    // Getter and Setter for groupName
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    // Getter and Setter for expenseName
    public String getExpenseName() {
        return expenseName;
    }

    public void setExpenseName(String expenseName) {
        this.expenseName = expenseName;
    }

    // Getter and Setter for expenseId
    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }
}
