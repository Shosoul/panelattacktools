package com.shosoul;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

public class PanelAttackStage {
    private static final String DEFAULT_ID_PREFIX = "pa_stages";
    private Path configPath;
    private JSONObject configjson;
    private String id;
    private String name;

    private boolean isEnabled;
    private boolean isDefault;
    private boolean isDisabledByGrandParent;

    /**
     * @return whether or not the stage is a default stage
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * A class containing information about a stage.
     * 
     * @param configPath the path to the character config.json file
     * @throws IOException if the configPath does not lead to a valid config file
     */
    public PanelAttackStage(Path configPath) throws IOException {
        this.configPath = configPath;
        configjson = new JSONObject(Files.readString(configPath));
        id = configjson.getString("id");
        name = configjson.getString("name");
        isEnabled = !configPath.getParent().getFileName().toString().startsWith("__");
        isDefault = id.startsWith(DEFAULT_ID_PREFIX);
    }

    public PanelAttackStage(Path configPath, JSONObject configjson) {
        this.configPath = configPath;
        this.configjson = configjson;
        id = configjson.getString("id");
        name = configjson.getString("name");
        isEnabled = !configPath.getParent().getFileName().toString().startsWith("__");
        isDefault = id.startsWith(DEFAULT_ID_PREFIX);
    }

    /**
     * @return the configPath
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * @param configPath the configPath to set
     */
    public void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    /**
     * @return the configjson
     */
    public JSONObject getConfigjson() {
        return configjson;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return if the stage is enabled or not
     */
    public boolean isEnabled() {
        isEnabled = !configPath.getParent().getFileName().toString().startsWith("__");
        return isEnabled;
    }

    /**
     * @param isEnabled the isEnabled to set
     */
    private void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public Path getStageFolder() {
        return configPath.getParent();
    }

    /**
     * Toggles the stage.
     * 
     * @return if the stage was toggled successfully.
     */
    public boolean toggleStage() {
        String oldDirName = getStageFolder().getFileName().toString();
        try {
            String newDirName;
            if (oldDirName.startsWith("__")) {
                newDirName = oldDirName.substring(2);
            } else {
                newDirName = "__" + oldDirName;
            }
            Path newParentPath = Files.move(getStageFolder(), getStageFolder().resolveSibling(newDirName));
            setConfigPath(newParentPath.resolve("config.json"));
            System.out.println(configPath.toAbsolutePath());
            setEnabled(!isEnabled);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public boolean isDisabledByGrandParent() {
        Path relativePath = Main.getPanelAttackDir().resolve("stages").relativize(configPath.getParent().getParent());
        if (relativePath.toString().contains("__")) {
            isDisabledByGrandParent = true;
        }
        return isDisabledByGrandParent;

    }

    public static List<PanelAttackStage> getStages(boolean addDefaults) {
        List<PanelAttackStage> stageArrayList = new ArrayList<>();
        int failedLoads = 0;
        // Find all the config.json files to determine stage folders
        try (Stream<Path> pathStream = Files
                .walk(Paths.get(Main.getAppDataDirectory()).resolve("Panel Attack").resolve("stages"))) {
            List<Path> configPaths = new ArrayList<>();
            pathStream.parallel()// parallel streams are better
                    .filter(Files::isRegularFile)// check if not a directory
                    .filter(f -> f.toFile().getAbsolutePath().endsWith("config.json"))// find files
                    .forEachOrdered(configPaths::add);

            for (Path path : configPaths) {
                try {
                    PanelAttackStage stage = new PanelAttackStage(path);
                    stageArrayList.add(stage);
                } catch (Exception e) {

                    try {
                        JSONObject repairedjson = Main.repairjson(Files.readString(path));
                        if (repairedjson != null) {
                            PanelAttackStage repairedStage = new PanelAttackStage(path, repairedjson);
                            stageArrayList.add(repairedStage);
                        } else {
                            failedLoads += 1;
                        }

                    } catch (Exception e1) {
                        failedLoads += 1;
                    }

                }
            }
            List<String> subIDList = new ArrayList<>();
            for (PanelAttackStage panelAttackStage : stageArrayList) {
                JSONArray array = panelAttackStage.getConfigjson().optJSONArray("sub_ids");
                if (array != null) {
                    array.toList().forEach(subID -> subIDList.add((String) subID));
                }
            }
            for (String subid : subIDList) {
                stageArrayList.removeIf(stage -> stage.getId().equals(subid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * If the modloader should also list default stages. If not, remove them.
         */
        if (!addDefaults) {
            stageArrayList.removeIf(PanelAttackStage::isDefault);
        }
        // TODO Sort by mod or alphabetically or something
        return stageArrayList;
    }

}
