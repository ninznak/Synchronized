package ru.alex;

import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static final Map<Integer, Integer> sizeToFreq = new HashMap<>();

    public static void main(String[] args) throws InterruptedException {
        int threadsTotal = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Callable<Void>> tasks = new ArrayList<>();

        Thread maxPrinter = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    synchronized (sizeToFreq) {
                        sizeToFreq.wait();
                        if (Thread.interrupted()) {
                            throw new InterruptedException();
                        }
                        printCurrentLeader();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        maxPrinter.start();

        for (int i = 0; i < threadsTotal; i++) {
            tasks.add(() -> {
                String route = generateRoute("RLRFR", 100);
                int totalChars = countChar(route, "R");

                synchronized (sizeToFreq) {
                    sizeToFreq.merge(totalChars, 1, Integer::sum);
                    sizeToFreq.notify();
                }
                return null;
            });
        }

        executorService.invokeAll(tasks);
        executorService.shutdown();

        maxPrinter.interrupt();

        synchronized (sizeToFreq) {
            sizeToFreq.notify();
        }

        maxPrinter.join();

        printFinalResult();
    }

    private static void printCurrentLeader() {
        if (sizeToFreq.isEmpty()) {
            return;
        }

        Map.Entry<Integer, Integer> maxEntry = Collections.max(
                sizeToFreq.entrySet(),
                Map.Entry.comparingByValue()
        );
        System.out.println("Текущий лидер: " + maxEntry.getKey() +
                " (встретилось " + maxEntry.getValue() + " раз)");
    }

    private static void printFinalResult() {
        synchronized (sizeToFreq) {
            if (sizeToFreq.isEmpty()) {
                System.out.println("Коллекция пустая!");
                return;
            }

            Map.Entry<Integer, Integer> maxResult = Collections.max(
                    sizeToFreq.entrySet(),
                    Map.Entry.comparingByValue()
            );

            System.out.println("\nФинальный результат:");
            System.out.printf("Самое частое количество повторений %s (встретилось %s раз)%n",
                    maxResult.getKey(), maxResult.getValue());

            System.out.println("Другие размеры:");
            sizeToFreq.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(maxResult.getKey()))
                    .forEach(entry -> System.out.printf("- %d (%d раз)%n",
                            entry.getKey(), entry.getValue()));
        }
    }

    public static int countChar(String route, String letter) {
        return route.length() - route.replace(letter, "").length();
    }

    public static String generateRoute(String letters, int length) {
        Random random = new Random();
        StringBuilder route = new StringBuilder();
        for (int i = 0; i < length; i++) {
            route.append(letters.charAt(random.nextInt(letters.length())));
        }
        return route.toString();
    }
}
