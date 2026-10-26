
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Disassembler
{
    static final int TEXT_BASE = 0x00400000;
    private static final String[] REGISTERS =
            {
                    "zero", "at", "v0", "v1", "a0", "a1", "a2", "a3",
                    "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
                    "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
                    "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"
            };
    public static void main(String[] args)
    {
        if (args.length == 0)
        {
            System.out.println("Usage: java Disassembler file.mhc-bin");
            return;
        }
        String file = args[0];
        String base = file;
        if (file.endsWith(".mhc-ascii"))
        {
            base = file.substring(0, file.length() - 10);
        }
        else if (file.endsWith(".mhc-bin"))
        {
            base = file.substring(0, file.length() - 8);
        }
        else if (file.endsWith(".ascii"))
        {
            base = file.substring(0, file.length() - 6);
        }
        else if (file.endsWith("-ascii"))
        {
            base = file.substring(0, file.length() - 6);
        }
        try
        {
            int[] words;
            if (file.endsWith("ascii") || file.endsWith("-ascii"))
            {
                words = readAscii(file);
            }
            else
            {
                words = readBinary(file);
            }
            disassemble(words, base + "_disassembled.asm");
            System.out.println("Disassembly successful.");
        }
        catch (IOException | IllegalArgumentException e)
        {
            System.out.println("Disassembly failed: " + e.getMessage());
        }
    }

    static void disassemble(int[] words, String outputFile) throws IOException
    {
        Set<Integer> targets = new HashSet<>();
        int pc = TEXT_BASE;
        for (int word : words)
        {
            int opcode = word >>> 26;
            if (opcode == 2)
            {
                int target = ((pc + 4) & 0xF0000000) | ((word & 0x03FFFFFF) << 2);
                targets.add(target);
            }
            else if (opcode == 4 || opcode == 5)
            {
                int immediate = (short) (word & 0xFFFF);
                targets.add(pc + 4 + immediate * 4);
            }
            pc += 4;
        }
        try (FileWriter out = new FileWriter(outputFile))
        {
            out.write(".text\n");
            pc = TEXT_BASE;
            for (int word : words)
            {
                if (targets.contains(pc))
                {
                    out.write(label(pc) + ":\n");
                }
                out.write("    " + decode(word, pc) + "\n");
                pc += 4;
            }
        }
    }

    private static String decode(int word, int pc)
    {
        if (word == 0x0000000C)
        {
            return "syscall";
        }
        int opcode = word >>> 26;
        int rs = (word >>> 21) & 31;
        int rt = (word >>> 16) & 31;
        int immediate = (short) (word & 0xFFFF);
        if (opcode == 0)
        {
            int rd = (word >>> 11) & 31;
            int funct = word & 63;
            switch (funct)
            {
                case 0x21:
                {
                    return "addu $" + REGISTERS[rd] + ", $" + REGISTERS[rs] + ", $" + REGISTERS[rt];
                }
                case 0x1A:
                {
                    return "div $" + REGISTERS[rs] + ", $" + REGISTERS[rt];
                }
                case 0x10:
                {
                    return "mfhi $" + REGISTERS[rd];
                }
                default:
                {
                    throw new IllegalArgumentException("Unsupported R-Type instruction at " + label(pc));
                }
            }
        }
        switch (opcode)
        {
            case 8:
            {
                return "addi $" + REGISTERS[rt] + ", $" + REGISTERS[rs] + ", " + immediate;
            }
            case 4:
            case 5:
            {
                int target = pc + 4 + immediate * 4;
                return (opcode == 4 ? "beq " : "bne ") + "$" + REGISTERS[rs] + ", $" + REGISTERS[rt] + ", " + label(target);
            }
            case 2:
            {
                int target = ((pc + 4) & 0xF0000000) | ((word & 0x03FFFFFF) << 2);
                return "j " + label(target);
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported opcode " + opcode + " at " + label(pc));
            }
        }
    }

    private static int[] readAscii(String file) throws IOException
    {
        List<Integer> words = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(file)))
        {
            line = line.trim();
            if (line.isEmpty())
            {
                continue;
            }
            if (!line.matches("[01]{32}"))
            {
                throw new IOException("Invalid 32-bit binary instruction: " + line);
            }
            words.add((int) Long.parseLong(line, 2));
        }
        int[] result = new int[words.size()];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = words.get(i);
        }
        return result;
    }

    private static int[] readBinary(String file) throws IOException
    {
        byte[] bytes = Files.readAllBytes(Paths.get(file));
        if (bytes.length % 4 != 0)
        {
            throw new IOException("Binary file length must be divisible by 4.");
        }
        int[] words = new int[bytes.length / 4];
        for (int i = 0; i < words.length; i++)
        {
            int p = i * 4;
            words[i] = ((bytes[p] & 255) << 24) | ((bytes[p + 1] & 255) << 16) | ((bytes[p + 2] & 255) << 8) | (bytes[p + 3] & 255);
        }
        return words;
    }

    private static String label(int address)
    {
        return String.format("L_%08X", address);
    }
}
