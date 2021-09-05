package client;


import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class History {

    private String fileName;

    public String load(String login) {
        if (!login.isEmpty()) {
            fileName = login + ".txt";
            Path path = Paths.get(fileName);
            if (Files.exists(path)) {
                System.out.println("load file " + fileName);
                try (Stream<String> lines = Files.lines(path)) {
                    return lines.collect(Collectors.joining("\n"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else
            fileName = "";
        return "";
    }

    public void save(String content) {
        if (fileName.isEmpty())
            return;
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(content);
            System.out.println("save file " + fileName);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
