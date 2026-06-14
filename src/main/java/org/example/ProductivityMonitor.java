package org.example;

import java.util.Scanner;

public class ProductivityMonitor {

    private static final String KONAMI_CODE = "UUDDLRLRBA";
    private static StringBuilder inputBuffer = new StringBuilder();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("🖥️  Productivity Monitor v1.0");
        System.out.println("Type away... (or try the Konami Code 👀)");
        System.out.println("Press 'q' to quit\n");

        while (true) {
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("q")) {
                System.out.println("Goodbye! May your code compile on the first try 🙏");
                break;
            }

            // Process each character
            for (char c : input.toUpperCase().toCharArray()) {
                checkInput(c);
            }
        }

        scanner.close();
    }

    public static void checkInput(char key) {
        inputBuffer.append(key);

        // Keep only last 10 characters
        if (inputBuffer.length() > 10) {
            inputBuffer.deleteCharAt(0);
        }

        if (inputBuffer.toString().equals(KONAMI_CODE)) {
            activateEasterEgg();
            inputBuffer = new StringBuilder(); // Reset
        }
    }

    private static void activateEasterEgg() {
        System.out.println("\n🎮 KONAMI CODE ACTIVATED! 🎮\n");
        System.out.println("⚡ UNLIMITED PRODUCTIVITY UNLOCKED ⚡");
        System.out.println("\n   ▄████████████▄");
        System.out.println("  █  IT'S NOT A  █");
        System.out.println("  █  BUG, IT'S A █");
        System.out.println("  █   ✨FEATURE✨ █");
        System.out.println("   ▀████████████▀\n");
        System.out.println("💪 You now have +30 Stack Overflow tabs");
        System.out.println("☕ Coffee supply: INFINITE");
        System.out.println("🐛 Bugs fixed: Yes");
        System.out.println("📝 Code reviews: They love it\n");
        playCelebration();
    }

    private static void playCelebration() {
        String[] memes = {
                "This is the way. 🛡️",
                "Task failed successfully ✅",
                "But it works on MY machine... 🤷",
                "git commit -m 'fixed stuff' && git push --force 🚀"
        };

        System.out.println("💬 Random dev wisdom: " +
                memes[(int)(Math.random() * memes.length)]);
    }
}