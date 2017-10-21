package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author
 */
public class Main {

    static String[] commands;
    static String sysDir = System.getProperty("user.dir");
    static CommitTree mainTree = null;
    static Stage mainStage = null;


    public static void init() {
        initDir();
        mainTree = new CommitTree();
        mainStage = new Stage();
        mainStage.setCurrentBlobs(mainTree.getCurrentBranchCommit());
        writeFile("", "config.bin", mainTree);
        writeFile("", "config_stage.bin", mainStage);
    }

    public static void add(String filename) {
        mainStage.add(filename, mainTree.getCurrentBranchCommit());
    }

    public static void commit(String msg) {
        if (mainStage.getStagedAdded().isEmpty() && mainStage.getStagedRemoved().isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else {
            mainTree.addToCommitTree(msg, mainStage.getStagedAdded(), mainStage.getStagedRemoved());
        }
    }

    public static void rm(String filename) {
        mainStage.remove(filename, mainTree.getCurrentBranchCommit());
    }

    public static void log() {
        System.out.println(mainTree.log());
    }

    public static void globalLog() {
        System.out.println(mainTree.globalLog());
    }

    public static void find(String msg) {
        String commits = mainTree.find(msg);
        if (commits.length() < 1) {
            System.out.println("Found no commit with that message.");
        } else {
            System.out.println(commits);
        }


    }

    public static void status() {
        String[] fileStates = mainStage.status();
        System.out.println("=== Branches ===");
        System.out.println(mainTree.status());
        System.out.println("=== Staged Files ===");
        System.out.println(fileStates[0]);
        System.out.println("=== Removed Files ===");
        System.out.println(fileStates[1]);
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println(fileStates[2]);
        System.out.println("=== Untracked Files ===");
        System.out.println(fileStates[3]);

//        for (String s : mainTree.getCurrentBranchCommit().getBlobs().keySet()){
//            System.out.println("Bloob " + s);
//        }
    }

    public static void checkOutFile(String fileName) {
        mainTree.checkOutFile(fileName);
    }

    public static void checkOutCommit(String commmitID, String filename) {
        mainTree.checkOutCommit(commmitID, filename);
    }

    public static void checkOutBranch(String branchName) {
        mainTree.checkOutBranch(branchName);
    }

    public static void branch(String branchName) {
        mainTree.branch(branchName);
    }

    public static void rmBranch(String branchName) {
        mainTree.rmBranch(branchName);
    }


    public static void reset(String commitID) {
        mainTree.reset(commitID);
        mainStage.clear();
    }

    public static void merge(String branchName) {
        mainTree.merge(branchName, Main.mainStage);
    }

    public static void initDir() {
        Path mainPath = Paths.get(sysDir, ".gitlet");
        Path commitsPath = Paths.get(sysDir, ".gitlet", ".commits");
        Path blobsPath = Paths.get(sysDir, ".gitlet", ".blobs");
        Path tempBlobsPath = Paths.get(sysDir, ".gitlet", ".temp_blobs");
        if (!Files.exists(mainPath)) {
            //creating main directory
            new File(mainPath.toString()).mkdirs();
            //creating commits directory inside the main directory
            new File(commitsPath.toString()).mkdirs();
            //creating blobs directory inside the main directory
            new File(blobsPath.toString()).mkdirs();
            //creating temp blobs directory(added but not committed blobs) inside the main directory
            new File(tempBlobsPath.toString()).mkdirs();
        } else {
            String e = "A gitlet version-control system already exists in the current directory.";
            System.out.println(e);
            System.exit(0);
        }
    }

    public static Object readFile(String dir, String fileName) {
        Object obj = null;
        if (dir != null && fileName != null) {
            Path path = Paths.get(sysDir, ".gitlet", dir);
            path = path.resolve(fileName);
            try {
                ObjectInputStream objInput = new ObjectInputStream(
                        new FileInputStream(path.toString()));
                while (true) {
                    obj = objInput.readObject();
                    objInput.close();
                    break;
                }
            } catch (FileNotFoundException e) {
                System.out.println("File does not exist.");
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.out.println("ClassNotFoundException: " + e.getMessage());
            }
        }
        return obj;
    }

    public static ArrayList<Object> readFiles(String dir) {
        ArrayList<Object> objs = new ArrayList<>();
        if (dir != null) {
            Path path = Paths.get(sysDir, ".gitlet", dir);

            File[] files = new File(path.toString()).listFiles();
            ArrayList<String> fileNames = new ArrayList<>();
            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }

            for (String fileName : fileNames) {
                objs.add(readFile(dir, fileName));
            }
        } else {
            System.out.println("Null directory.");
        }
        return objs;
    }

    public static void writeFile(String dir, String fileName, Object obj) {
        if (dir != null && fileName != null) {
            Path path = Paths.get(sysDir, ".gitlet", dir);

            if (!Files.exists(path)) {
                new File(path.toString()).mkdirs();
            }
            path = path.resolve(fileName);
            try {
                ObjectOutputStream objOut = new ObjectOutputStream(
                        new FileOutputStream(path.toString()));
                objOut.writeObject(obj);
                objOut.close();
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        } else if (dir == null) {
            System.out.println("Null directory.");
        } else {
            System.out.println("Null filename.");
        }
    }

    public static void deleteFile(String dir, String fileName) {
        if (dir != null && fileName != null) {
            Path path = Paths.get(sysDir, ".gitlet", dir);
            path = path.resolve(fileName);
            try {
                Files.deleteIfExists(path);
            } catch (NoSuchFileException e) {
                System.out.println("NoSuchFileException: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        } else if (dir == null) {
            System.out.println("Null directory.");
        } else {
            System.out.println("Null filename.");
        }
    }

    public static void deleteFiles(String dir, ArrayList<String> fileNames) {
        if (dir != null) {
            for (String fileName : fileNames) {
                deleteFile(dir, fileName);
            }
        } else {
            System.out.println("Null directory.");
        }
    }

    // Read File in the current working directory and returns its content to String
    public static String readCWDFileToString(String dir, String fileName) {
        byte[] encoded = null;

        if (dir != null && fileName != null) {
            Path path = Paths.get(sysDir, dir);
            path = path.resolve(fileName);

            try {
                encoded = Files.readAllBytes(path);
            } catch (IOException e) {
                //System.out.println("IOException: " + e.getMessage());
                System.out.println("File does not exist.");
            }
        }

        return new String(encoded);
    }

    public static ArrayList<String> getCWDFileNames(String dir) {
        ArrayList<String> namesToReturn = new ArrayList<>();
        if (dir != null) {
            Path path = Paths.get(sysDir, dir);
            File[] files = new File(path.toString()).listFiles();

            for (File file : files) {
                if (file.isFile()) {
                    namesToReturn.add(file.getName());
                }
            }
        }
        return namesToReturn;
    }


    public static ArrayList<String> getFileNames(String dir) {
        ArrayList<String> namesToReturn = new ArrayList<>();
        if (dir != null) {
            Path path = Paths.get(dir);
            File[] files = new File(path.toString()).listFiles();

            for (File file : files) {
                if (file.isFile()) {
                    namesToReturn.add(file.getName());
                }
            }
        }
        return namesToReturn;
    }

    // Write String into Blob in the current working directory
    public static void writeToCWDFile(String dir, String fileName, String content) {
        if (dir != null && fileName != null) {
            Path path = Paths.get(sysDir, dir);

            if (!Files.exists(path)) {
                new File(path.toString()).mkdirs();
            }

            path = path.resolve(fileName);
            File f = new File(path.toString());
            Utils.writeContents(f, content.getBytes());

        } else if (dir == null) {
            System.out.println("Null directory.");
        } else {
            System.out.println("Null filename.");
        }
    }

    // Delete specific Blob in the current working directory
    public static void deleteCWDFile(String dir, String fileName) {
        if (dir != null && fileName != null) {
            Path path = Paths.get(sysDir, dir);
            path = path.resolve(fileName);
            try {
                Files.deleteIfExists(path);
            } catch (NoSuchFileException e) {
                System.out.println("NoSuchFileException: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IOException: " + e.getMessage());
            }
        } else if (dir == null) {
            System.out.println("Null directory.");
        } else {
            System.out.println("Null filename.");
        }
    }

    // Delete given Blobs in the current working directory
    public static void deleteCWDFiles(String dir, ArrayList<String> fileNames) {
        if (dir != null) {
            for (String fileName : fileNames) {
                deleteCWDFile(dir, fileName);
            }
        } else {
            System.out.println("Null directory.");
        }
    }

    public static void commandRunner(String[] args) {
        commands = args;
        if (commands.length < 1) {
            throw new IllegalArgumentException("Please enter a command.");
        }
        switch (commands[0]) {
            case "init":
                init();
                break;
            case "add":
                if (commands.length == 2) {
                    add(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "commit":
                if (commands.length < 2 || commands[1].equals("")) {
                    System.out.println("Please enter a commit message.");
                } else if (commands.length == 2) {
                    commit(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "rm":
                if (commands.length == 2) {
                    rm(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "log":
                log();
                break;
            case "global-log":
                globalLog();
                break;
            case "find":
                if (commands.length == 2) {
                    find(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "status":
                status();
                break;
            case "checkout":
                if (commands.length == 3 && commands[1].equals("--")) {
                    checkOutFile(commands[2]);
                } else if (commands.length == 4 && commands[2].equals("--")) {
                    checkOutCommit(commands[1], commands[3]);
                } else if (commands.length == 2) {
                    checkOutBranch(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "branch":
                if (commands.length == 2) {
                    branch(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "rm-branch":
                if (commands.length == 2) {
                    rmBranch(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "reset":
                if (commands.length == 2) {
                    reset(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            case "merge":
                if (commands.length == 2) {
                    merge(commands[1]);
                } else {
                    throw new IllegalArgumentException("Incorrect operands.");
                }
                break;
            default:
                throw new IllegalArgumentException("No command with that name exists.");
        }
    }

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */
    public static void main(String... args) {

        try {
            if (Files.exists(Paths.get(sysDir, ".gitlet").resolve("config.bin"))) {
                mainTree = (CommitTree) readFile("", "config.bin");
                mainStage = (Stage) readFile("", "config_stage.bin");
            }
            commandRunner(args);
            writeFile("", "config.bin", mainTree);
            writeFile("", "config_stage.bin", mainStage);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null) {
                System.out.println(e.getMessage());
            }
        } catch (NullPointerException f) {
            if (f.getMessage() != null) {
                System.out.println(f.getMessage());
            }
        }
    }
}
