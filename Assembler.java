
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Assembler
{
    static final int TEXT_BASE = 0x00400000;
    private static final String[] REGISTERS =
            {
                    "zero", "at", "v0", "v1", "a0", "a1", "a2", "a3",
                    "t0", "t1", "t2", "t3", "t4", "t5", "t6", "t7",
                    "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7",
                    "t8", "t9", "k0", "k1", "gp", "sp", "fp", "ra"
            };
    private static class Instruction
    {
        String text;
        int address;
        int line;
        Instruction(String text, int address, int line)
        {
            this.text = text;
            this.address = address;
            this.line = line;
        }
    }
    private final Map<String, Integer> labels = new HashMap<>();
    private final List<Instruction> instructions = new ArrayList<>();
    private int pc = TEXT_BASE;
    public static void main(String[] args)
    {
        if (args.length == 0)
        {
            System.out.println("No file provided");
            return;
        }
        String file = args[0];
        String base = file.endsWith(".asm") ? file.substring(0, file.length() - 4) : file;
        try
        {
            byte[] output = assemble(new String(Files.readAllBytes(Paths.get(file))));
            try (FileOutputStream out = new FileOutputStream(base + ".mhc-bin"))
            {
                out.write(output);
            }
            try (FileWriter out = new FileWriter(base + ".mhc-ascii"))
            {
                for (int i = 0; i < output.length; i += 4)
                {
                    int word = readWord(output, i);
                    out.write(String.format("%32s", Integer.toBinaryString(word)).replace(' ', '0') + "\n");
                }
            }
            System.out.println("Assembly successful.");
        }
        catch (IOException | IllegalArgumentException e)
        {
            System.out.println("Assembly failed: " + e.getMessage());
        }
    }

    static byte[] assemble(String source)
    {
        Assembler assembler = new Assembler();
        assembler.firstPass(source);
        if (assembler.instructions.isEmpty())
        {
            throw new IllegalArgumentException("No instructions found.");
        }
        byte[] text = new byte[assembler.instructions.size() * 4];
        for (int i = 0; i < assembler.instructions.size(); i++)
        {
            Instruction instruction = assembler.instructions.get(i);
            try
            {
                int word = assembler.encode(instruction);
                text[i * 4] = (byte) (word >>> 24);
                text[i * 4 + 1] = (byte) (word >>> 16);
                text[i * 4 + 2] = (byte) (word >>> 8);
                text[i * 4 + 3] = (byte) word;
            }
            catch (IllegalArgumentException e)
            {
                throw new IllegalArgumentException("Line " + instruction.line + ": " + e.getMessage());
            }
        }
        return text;
    }

    private void firstPass(String source)
    {
        if (source.startsWith("\uFEFF"))
        {
            source = source.substring(1);
        }
        String[] lines = source.split("\\r\\n|\\r|\\n");
        for (int i = 0; i < lines.length; i++)
        {
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty())
            {
                continue;
            }
            if (line.equals(".text"))
            {
                continue;
            }
            if (line.startsWith("."))
            {
                throw new IllegalArgumentException("Line " + (i + 1) + ": Unsupported directive.");
            }
            while (true)
            {
                int colon = line.indexOf(':');
                if (colon < 0)
                {
                    break;
                }
                String name = line.substring(0, colon).trim();
                if (!name.matches("[A-Za-z_][A-Za-z0-9_]*"))
                {
                    throw new IllegalArgumentException("Line " + (i + 1) + ": Invalid label.");
                }
                if (labels.containsKey(name))
                {
                    throw new IllegalArgumentException("Line " + (i + 1) + ": Duplicate label " + name);
                }
                labels.put(name, pc);
                line = line.substring(colon + 1).trim();
                if (line.isEmpty())
                {
                    break;
                }
            }
            if (line.isEmpty())
            {
                continue;
            }
            validateInstructionName(line, i + 1);
            instructions.add(new Instruction(line, pc, i + 1));
            pc += 4;
        }
    }

    private void validateInstructionName(String text, int line)
    {
        String op = text.split("\\s+", 2)[0].toLowerCase();
        switch (op)
        {
            case "addi":
            case "addu":
            case "div":
            case "mfhi":
            case "beq":
            case "bne":
            case "j":
            case "syscall":
                return;
            default:
                throw new IllegalArgumentException("Line " + line + ": Unsupported instruction " + op);
        }
    }

    private int encode(Instruction instruction)
    {
        String[] parts = instruction.text.trim().split("\\s+", 2);
        String op = parts[0].toLowerCase();
        String[] operands = getOperands(parts);
        switch (op)
        {
            case "addi":
            {
                count(operands, 3);
                int immediate = number(operands[2]);
                range(immediate, -32768, 32767);
                return iType(8, reg(operands[1]), reg(operands[0]), immediate);
            }
            case "addu":
            {
                count(operands, 3);
                return rType(reg(operands[1]), reg(operands[2]), reg(operands[0]), 0, 0x21);
            }
            case "div":
            {
                count(operands, 2);
                return rType(reg(operands[0]), reg(operands[1]), 0, 0, 0x1A);
            }
            case "mfhi":
            {
                count(operands, 1);
                return rType(0, 0, reg(operands[0]), 0, 0x10);
            }
            case "beq":
            case "bne":
            {
                count(operands, 3);
                Integer target = labels.get(operands[2]);
                if (target == null)
                {
                    throw new IllegalArgumentException("Undefined label: " + operands[2]);
                }
                int difference = target - (instruction.address + 4);
                if (difference % 4 != 0)
                {
                    throw new IllegalArgumentException("Unaligned branch target.");
                }
                int offset = difference / 4;
                range(offset, -32768, 32767);
                return iType(op.equals("beq") ? 4 : 5, reg(operands[0]), reg(operands[1]), offset);
            }
            case "j":
            {
                count(operands, 1);
                Integer target = labels.get(operands[0]);
                if (target == null)
                {
                    throw new IllegalArgumentException("Undefined label: " + operands[0]);
                }
                if ((target & 3) != 0 || ((target >>> 28) != ((instruction.address + 4) >>> 28)))
                {
                    throw new IllegalArgumentException("Jump target out of range or unaligned.");
                }
                return (2 << 26) | ((target >>> 2) & 0x03FFFFFF);
            }
            case "syscall":
            {
                count(operands, 0);
                return 0x0000000C;
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported instruction: " + op);
            }
        }
    }

    private static int rType(int rs, int rt, int rd, int shamt, int funct)
    {
        return (rs << 21) | (rt << 16) | (rd << 11) | (shamt << 6) | funct;
    }

    private static int iType(int opcode, int rs, int rt, int immediate)
    {
        return (opcode << 26) | (rs << 21) | (rt << 16) | (immediate & 0xFFFF);
    }

    private static int reg(String name)
    {
        name = name.trim().toLowerCase();
        if (name.startsWith("$"))
        {
            name = name.substring(1);
        }
        if (name.equals("s8"))
        {
            return 30;
        }
        for (int i = 0; i < REGISTERS.length; i++)
        {
            if (name.equals(REGISTERS[i]) || name.equals("" + i))
            {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown register: $" + name);
    }

    private static int number(String text)
    {
        text = text.trim();
        if (text.length() >= 2 && text.charAt(0) == '\'' && text.charAt(text.length() - 1) == '\'')
        {
            String value = text.substring(1, text.length() - 1);
            switch (value)
            {
                case "\\n":
                    return 10;
                case "\\t":
                    return 9;
                case "\\r":
                    return 13;
                case "\\0":
                    return 0;
                case "\\\\":
                    return 92;
                case "\\'":
                    return 39;
                default:
                {
                    if (value.length() == 1 && value.charAt(0) <= 127)
                    {
                        return value.charAt(0);
                    }
                    throw new IllegalArgumentException("Invalid character literal: " + text);
                }
            }
        }
        try
        {
            if (text.startsWith("0x") || text.startsWith("0X"))
            {
                return Integer.parseInt(text.substring(2), 16);
            }
            return Integer.parseInt(text);
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("Invalid number: " + text);
        }
    }

    private static String[] getOperands(String[] parts)
    {
        if (parts.length < 2 || parts[1].trim().isEmpty())
        {
            return new String[0];
        }
        return parts[1].split("\\s*,\\s*", -1);
    }

    private static void count(String[] operands, int expected)
    {
        if (operands.length != expected)
        {
            throw new IllegalArgumentException("Expected " + expected + " operand(s).");
        }
        for (String operand : operands)
        {
            if (operand.isEmpty())
            {
                throw new IllegalArgumentException("Empty operand.");
            }
        }
    }

    private static int range(int value, int min, int max)
    {
        if (value < min || value > max)
        {
            throw new IllegalArgumentException("Immediate out of range: " + value);
        }
        return value;
    }

    private static String stripComment(String line)
    {
        boolean inChar = false;
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (inChar)
            {
                if (escaped)
                {
                    escaped = false;
                }
                else if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '\'')
                {
                    inChar = false;
                }
            }
            else if (c == '\'')
            {
                inChar = true;
            }
            else if (c == '#')
            {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int readWord(byte[] bytes, int index)
    {
        return ((bytes[index] & 255) << 24) | ((bytes[index + 1] & 255) << 16) | ((bytes[index + 2] & 255) << 8) | (bytes[index + 3] & 255);
    }
}
