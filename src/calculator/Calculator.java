package calculator;

import java.util.Scanner;

public class Calculator {

    private final Scanner scanner;

    // ANSI colors for better UI
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";

    public Calculator() {
        scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println(BLUE + "==========================" + RESET);
        System.out.println(BLUE + "   Welcome to Calculator   " + RESET);
        System.out.println(BLUE + "==========================" + RESET);

        while (true) {
            try {
                showMenu();
                System.out.print("Choose an operation (1-7): ");
                int operation = scanner.nextInt();

                if (operation == 7) {
                    System.out.println(GREEN + "✅ Exiting calculator. Goodbye!" + RESET);
                    break;
                }

                if (operation < 1 || operation > 7) {
                    System.out.println(RED + "❌ Invalid choice! Please choose between 1 and 7." + RESET);
                    continue;
                }

                System.out.print("Enter first number: ");
                double num1 = scanner.nextDouble();

                System.out.print("Enter second number: ");
                double num2 = scanner.nextDouble();

                double result = performOperation(operation, num1, num2);

                if (Double.isNaN(result)) {
                    System.out.println(RED + "❌ Invalid operation (e.g., division by zero)." + RESET);
                } else {
                    System.out.println(GREEN + "✅ Result: " + num1 + " " + getSymbol(operation) + " " + num2 + " = " + result + RESET);
                }

            } catch (Exception e) {
                System.out.println(RED + "❌ Error: Invalid input. Please enter numbers only." + RESET);
                scanner.nextLine(); // clear invalid input
            }
        }
    }

    private void showMenu() {
        System.out.println(YELLOW + "\nAvailable operations:" + RESET);
        System.out.println("1. Addition (+)");
        System.out.println("2. Subtraction (-)");
        System.out.println("3. Multiplication (*)");
        System.out.println("4. Division (/)");
        System.out.println("5. Modulus (%)");
        System.out.println("6. Power (^)");
        System.out.println("7. Exit");
    }

    private double performOperation(int operation, double num1, double num2) {
        return switch (operation) {
            case 1 -> num1 + num2;
            case 2 -> num1 - num2;
            case 3 -> num1 * num2;
            case 4 -> (num2 == 0) ? Double.NaN : (num1 / num2);
            case 5 -> (num2 == 0) ? Double.NaN : (num1 % num2);
            case 6 -> Math.pow(num1, num2);
            default -> Double.NaN;
        };
    }

    private String getSymbol(int operation) {
        return switch (operation) {
            case 1 -> "+";
            case 2 -> "-";
            case 3 -> "*";
            case 4 -> "/";
            case 5 -> "%";
            case 6 -> "^";
            default -> "?";
        };
    }

    public static void main(String[] args) {
        new Calculator().start();
    }
}
