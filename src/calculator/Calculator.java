package calculator;

import java.util.Scanner;

public class Calculator {

    private final Scanner scanner;

    public Calculator() {
        scanner = new Scanner(System.in);
    }

    public void start(){
        System.out.println("Welcome to Calculator!");

        while (true){
            try {
                System.out.println("Enter operation: ");
                System.out.println("1. Addition (+)");
                System.out.println("2. Subtraction (-)");
                System.out.println("3. Multiplication (*)");
                System.out.println("4. Division (/)");
                System.out.println("5. Exit");
                System.out.println("Choose an operation (1-5): ");

                int operation = scanner.nextInt();
                if (operation == 5) {
                    System.out.println("Exiting calculator. Goodbye!");
                    break;
                }

                System.out.print("Enter first number: ");
                double num1 = scanner.nextDouble();

                System.out.print("Enter second number: ");
                double num2 = scanner.nextDouble();

                double result = performOperation(operation, num1, num2);
                if (Double.isNaN(result)) {
                    System.out.println("Invalid operation or division by zero.");
                } else {
                    System.out.println("Result: " + result);
                }

            }catch (Exception e){
                System.out.println("Error: "+ e.getMessage());
                scanner.nextLine();
            }
        }
    }

    private double performOperation(int operation, double num1, double num2) {
        return switch (operation) {
            case 1 -> num1 + num2;
            case 2 -> num1 - num2;
            case 3 -> num1 * num2;
            case 4 -> (num2 == 0) ? Double.NaN : (num1 / num2);
            default -> Double.NaN;
        };
    }

    public static void main(String[] args) {
        Calculator calculator = new Calculator();
        calculator.start();
    }


}
