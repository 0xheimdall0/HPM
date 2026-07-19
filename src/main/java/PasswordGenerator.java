import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {
    protected static String generatePassword(PWDGenOptions opt) {
        String ambiguous = "il1Lo0O";

        // Gather all the characters that must be used according to the use
        List<String> sets = new ArrayList<>();
        if (opt.useLower) sets.add("abcdefghijklmnopqrstuvwxyz");
        if (opt.useUpper) sets.add("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        if (opt.useNumbers) sets.add("0123456789");
        if (opt.useSymbols) sets.add("!@#$%^&*()-_=+[]{}");
        if (sets.isEmpty()) return "";
        if (opt.excludeAmbiguous) {
            sets.replaceAll(source -> strip(source, ambiguous));
        }

        SecureRandom random = new SecureRandom();
        List<Character> chars = new ArrayList<>();

        // Guarantee one character from each enabled set
        for (String set : sets) {
            chars.add(set.charAt(random.nextInt(set.length())));
        }

        // Fill the rest with the requested sets
        String all = String.join("", sets);
        while (chars.size() < opt.length) {
            chars.add(all.charAt(random.nextInt(all.length())));
        }

        // Shuffle to guarantee randomness
        Collections.shuffle(chars, random);

        // Build the final string
        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }
    protected static String strip(String source, String remove) {
        StringBuilder sb = new StringBuilder();
        for (char c : source.toCharArray()) {
            if (remove.indexOf(c) < 0) sb.append(c);
        }
        return sb.toString();
    }
    protected static String generatePassword(int length) {
        PWDGenOptions o = new PWDGenOptions();
        o.length = length;
        return generatePassword(o);
    }
}
