package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));

    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File ADD_DIR = join(GITLET_DIR, "added");
    public static final File REMOVED = join(GITLET_DIR, "removed");
    public static final File COMMIT_DIR = join(GITLET_DIR, "commits");
    public static final File BLOB_DIR = join(GITLET_DIR, "blobs");
    public static final File CURR_BRANCH = join(GITLET_DIR, "currentBranch");
    public static final File BRANCHES = join(GITLET_DIR, "branches");
    public static final File TEMP = join(GITLET_DIR, "temp");

    /* TODO: fill in the rest of this class. */
    /**
     * Does required filesystem operations to allow for persistence.
     * (creates any necessary folders or files)
     *
     * Structure:
     *
     * .gitlet/ -- top level folder for all persistent data
     *    - added/ -- folder containing all of persistent data added
     *    - removed/ -- folder containing ...
     *    - commits/ -- folder containing commit objects
     *    - blobs/ -- folder containing blob objects
     *    - branches/ -- folder noting all our branches
     *    - current_branch -- file detailing the current branch
     */
    public static void setUpPersistence() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
        }
        if (!ADD_DIR.exists()) {
            ADD_DIR.mkdir();
        }
        if (!REMOVED.exists()) {
            HashSet<String> removed = new HashSet<>();
            writeObject(REMOVED, removed);
        }
        if (!COMMIT_DIR.exists()) {
            COMMIT_DIR.mkdir();
        }
        if (!BLOB_DIR.exists()) {
            BLOB_DIR.mkdir();
        }
        if (!join(GITLET_DIR, "branches").exists()) {
            HashMap<String, String> branches = new HashMap<>();
            branches.put("master", "placeholderHeadSHA");
            writeObject(BRANCHES, branches);
        }
        if (!join(GITLET_DIR, "currentBranch").exists()) {
            writeContents(CURR_BRANCH, "master");
        }
    }

    public static void initCommand() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        setUpPersistence();
        makeInitialCommit();
    }

    private static void makeInitialCommit() {
        Commit commit = new Commit();
        commit.saveCommit();
    }

    public static void makeCommit(String message) {
        Commit commit = new Commit(message);
        commit.saveCommit();
    }

    public static void addCommand(String filename) {
        if (!join(CWD, filename).exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        HashSet<String> removals = readObject(REMOVED, HashSet.class);
        if (removals.remove(filename)) {
            writeObject(REMOVED, removals);
        }

        File file = join(CWD, filename);
        if (checkIfChanged(file)) {
            File copiedFile = join(ADD_DIR, filename);
            copyContents(file, copiedFile);
        } else if (checkIfStaged(file)) {
            File staged = join(ADD_DIR, file.getName());
            staged.delete();
        }
    }

    public static void copyContents(File original, File duplicate) {
        byte[] copiedContent = readContents(original);
        writeContents(duplicate, copiedContent);
    }

    public static boolean checkIfStaged(File toBeChecked) {

        File staged = join(ADD_DIR, toBeChecked.getName());
        if (staged.exists()){
            return true;
        }
        return false;
    }

    public static Commit findCurrentCommit() {
        String currentBranch = readContentsAsString(CURR_BRANCH);
        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        String currentCommitSha = branches.get(currentBranch);
        Commit currentCommit = readObject(join(COMMIT_DIR, currentCommitSha), Commit.class);
        return currentCommit;
    }

    public static HashMap<String, String> findCommitBlobs() {
        Commit currentCommit = findCurrentCommit();
        HashMap<String, String> currentBlobs = currentCommit.getBlobs();
        return currentBlobs;
    }

    public static boolean checkIfChanged(File toBeChecked) {
        HashMap<String, String> currentBlobs = findCommitBlobs();
        String commitFileSha = currentBlobs.get(toBeChecked.getName());
        if (commitFileSha == null) {
            return true;
        }

        return !commitFileSha.equals(sha1(readContents(toBeChecked)));
    }

    public static void commitCommand(String mes) {
        if (ADD_DIR.listFiles().length == 0 && readObject(REMOVED, HashSet.class).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        makeCommit(mes);
    }

    public static void logCommand() {
        Commit currentCommit = findCurrentCommit();
        Commit p = currentCommit;

        while(true) {
            p.printCommit();
            String parentSHA = p.getParentSHA();
            if (parentSHA == null) {
                break;
            }
            p = readObject(join(COMMIT_DIR, parentSHA), Commit.class);
        }
    }

    public static void checkoutCommandHead(String fileName) {
        HashMap<String, String> blobs = findCommitBlobs();
        String commitFileSha = blobs.get(fileName);
        if (commitFileSha == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        copyContents(join(BLOB_DIR, commitFileSha), join(CWD, fileName));
    }

    public static void checkoutCommandCommit(String commitSHA, String fileName) {
        File commitFile = shortenedUID(commitSHA);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit commit = readObject(commitFile, Commit.class);
        HashMap<String, String> blobs = commit.getBlobs();
        String commitFileSha = blobs.get(fileName);
        if (commitFileSha == null) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        copyContents(join(BLOB_DIR, commitFileSha), join(CWD, fileName));
    }

    public static File shortenedUID(String uid) {
        List<String> commitSHAS = plainFilenamesIn(COMMIT_DIR);
        int len = uid.length();
        if (len < 6) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        } else if (len >= 40) {
            return join(COMMIT_DIR, uid);
        } else {
            for (String sha : commitSHAS) {
                if (uid.equals(sha.substring(0, len))) {
                    return join(COMMIT_DIR, sha);
                }
            }
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        return null;
    }

    public static void checkoutCommandBranch(String branchName) {
        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        if (readContentsAsString(CURR_BRANCH).equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        String headCommitSHA = branches.get(branchName);
        Commit headCommit = readObject(join(COMMIT_DIR, headCommitSHA), Commit.class);
        HashMap<String, String> headBlobs = headCommit.getBlobs();

        checkUntrackedCWD();
        updateCWD(headBlobs);
        clearStagingAreas();

        writeContents(CURR_BRANCH, branchName);
    }

    private static void checkUntrackedCWD() {
        HashMap<String, String> currentBlobs = findCommitBlobs();

        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (String fileName : cwdFiles) {
            if (currentBlobs.containsKey(fileName)) {
                String contents = sha1(readContents(join(CWD, fileName)));
                if (!(currentBlobs.get(fileName)).equals(contents)) {
                    if (!join(ADD_DIR, fileName).exists()) {
                        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                        System.exit(0);
                    } else if (!sha1(readContents(join(ADD_DIR, fileName))).equals(contents)) {
                        System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                        System.exit(0);
                    }
                }
            } else {
                String contents = sha1(readContents(join(CWD, fileName)));
                if (!join(ADD_DIR, fileName).exists()) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                } else if (!sha1(readContents(join(ADD_DIR, fileName))).equals(contents)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
    }

    private static void updateCWD(HashMap<String, String> blobs) {
        // deletes files in the CWD that are not in the blobs passed into
        // this function
        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (String fileName : cwdFiles) {
            if (!blobs.containsKey(fileName)) {
                join(CWD, fileName).delete();
            }
        }

        // copies files from the given blobs to the CWD
        for (String key : blobs.keySet()) {
            File blob = join(BLOB_DIR, blobs.get(key));
            File cwdFile = join(CWD, key);
            copyContents(blob, cwdFile);
        }
    }

    private static void clearStagingAreas() {
        File[] stagedForAdding = Repository.ADD_DIR.listFiles();
        for (File f : stagedForAdding) {
            f.delete();
        }

        HashSet<String> stagedForRemoval = readObject(Repository.REMOVED, HashSet.class);
        stagedForRemoval.clear();
        writeObject(Repository.REMOVED, stagedForRemoval);
    }

    public static void globalLogCommand() {

        File[] toBePrinted = COMMIT_DIR.listFiles();

        for (File f : toBePrinted) {
            readObject(f, Commit.class).printCommit();
        }
    }

    public static void findCommand(String message) {
        File[] toBePrinted = COMMIT_DIR.listFiles();
        Commit messageFinder;
        boolean messageFound = false;
        for (File f : toBePrinted) {
            messageFinder = readObject(f, Commit.class);
            if (messageFinder.getMessage().equals(message)) {
                System.out.println(f.getName());
                messageFound = true;
            }
        }
        if (!messageFound) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void statusCommand() {
        System.out.println("=== Branches ===");
        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        String currBranch = readContentsAsString(CURR_BRANCH);
        List<String> sorter = new ArrayList<>();
        for (String key : branches.keySet()) {
            sorter.add(key);
        }
        Collections.sort(sorter);
        for (String key : sorter) {
            if (key.equals(currBranch)) {
                System.out.print("*");
            }
            System.out.println(key);
        }
        System.out.println();

        System.out.println("=== Staged Files ===");
        File[] stagedFiles = ADD_DIR.listFiles();
        sorter = new ArrayList<>();
        for (File f : stagedFiles) {
            sorter.add(f.getName());
        }
        Collections.sort(sorter);
        for (String staged : sorter) {
            System.out.println(staged);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        HashSet<String> removedFiles = readObject(REMOVED, HashSet.class);
        sorter = new ArrayList<>();
        for (String key : removedFiles) {
            sorter.add(key);
        }
        Collections.sort(sorter);
        for (String removed : sorter) {
            System.out.println(removed);
        }

        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");

        System.out.println();

        System.out.println("=== Untracked Files ===");

        System.out.println();
    }

    public static void removeCommand(String filename) {
        File toBeRemoved = join(ADD_DIR, filename);
        boolean fileWasStaged = toBeRemoved.exists();
        if (fileWasStaged) {
            toBeRemoved.delete();
        }

        HashMap<String, String> currentBlobs = findCommitBlobs();
        if (currentBlobs.containsKey(filename)) {
            HashSet<String> removals = readObject(REMOVED, HashSet.class);
            removals.add(filename);
            writeObject(REMOVED, removals);
            File cwdFile = join(CWD, filename);
            if (cwdFile.exists()) {
                cwdFile.delete();
            }
        } else if (!fileWasStaged) {
            System.out.println("No reason to remove the file.");
        }
    }

    public static void branchCommand(String branchName) {
        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        branches.put(branchName, branches.get(readContentsAsString(CURR_BRANCH)));
        writeObject(BRANCHES, branches);
    }

    public static void removeBranchCommand(String branchName) {
        if (readContentsAsString(CURR_BRANCH).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        if (branches.containsKey(branchName)) {
            branches.remove(branchName);
        } else {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        writeObject(BRANCHES, branches);
    }

    public static void resetCommand(String commitID) {
        File desiredCommit = shortenedUID(commitID);
        if (!desiredCommit.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        checkUntrackedCWD();
        Commit checkoutCommit = readObject(join(COMMIT_DIR, commitID), Commit.class);
        HashMap<String, String> desiredBlobs = checkoutCommit.getBlobs();
        updateCWD(desiredBlobs);
        clearStagingAreas();


        HashMap<String, String> branches = readObject(BRANCHES, HashMap.class);
        String currentBranch = readContentsAsString(CURR_BRANCH);
        branches.put(currentBranch, commitID);
        writeObject(BRANCHES, branches);
    }
}
