package fr.robie.yamllibrary.file;

import fr.robie.yamllibrary.Configuration;
import fr.robie.yamllibrary.InvalidConfigurationException;
import fr.robie.yamllibrary.MemoryConfiguration;
import fr.robie.yamllibrary.util.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;

public abstract class FileConfiguration extends MemoryConfiguration {

    public FileConfiguration() {
        super();
    }

    public FileConfiguration(@Nullable Configuration defaults) {
        super(defaults);
    }

    public void save(@NotNull File file) throws IOException {
        Preconditions.checkNotNull(file, "File cannot be null");

        File parent = file.getCanonicalFile().getParentFile();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent.toPath());
        }

        String data = this.saveToString();

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(data);
        }
    }

    public void save(@NotNull String file) throws IOException {
        Preconditions.checkNotNull(file, "File cannot be null");

        this.save(new File(file));
    }

    @NotNull
    public abstract String saveToString();

    public void load(@NotNull File file) throws IOException, InvalidConfigurationException {
        Preconditions.checkNotNull(file, "File cannot be null");

        final FileInputStream stream = new FileInputStream(file);

        this.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    public void load(@NotNull Reader reader) throws IOException, InvalidConfigurationException {
        BufferedReader input = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);

        StringBuilder builder = new StringBuilder();

        try {
            String line;

            while ((line = input.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }
        } finally {
            input.close();
        }

        this.loadFromString(builder.toString());
    }

    public void load(@NotNull String file) throws IOException, InvalidConfigurationException {
        Preconditions.checkNotNull(file, "File cannot be null");

        this.load(new File(file));
    }

    public abstract void loadFromString(@NotNull String contents) throws InvalidConfigurationException;

    @NotNull
    @Override
    public FileConfigurationOptions options() {
        if (this.options == null) {
            this.options = new FileConfigurationOptions(this);
        }

        return (FileConfigurationOptions) this.options;
    }
}
