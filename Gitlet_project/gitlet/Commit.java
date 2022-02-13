package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.security.PrivateKey;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    private String parentSHA;
    private Date commitTime;
    // key is name of file (String), corresponding value is SHA-1 (String)
    private HashMap<String, String> blobs;

    /** The message of this Commit. */
    private String message;

    public Commit() {
        this.parentSHA = null;
        this.blobs = new HashMap<String, String>();
        this.message = "initial commit";
        this.commitTime = new Date(0);
    }

    public Commit(String m) {
        this.parentSHA = findParentSHA1();
        this.blobs = commitBlobs();
        this.message = m;
        this.commitTime = new Date();
    }

    public HashMap<String, String> getBlobs(){
        return blobs;
    }

    public String getParentSHA(){
        return parentSHA;
    }

    public Date getCommitTime(){
        return commitTime;
    }

    public String getMessage(){
        return message;
    }

    public void printCommit(){
        System.out.println("===");
        System.out.println("commit " + sha1(serialize(this)));
        System.out.println(String.format("Date: %1$tb %1$ta %1$te %1$tH:%1$tM:%1$tS %1$tY %1$tz", commitTime));
        System.out.println(message);
        System.out.println();
    }

    public void saveCommit() {
        String sha = sha1(serialize(this));
        File thisCommit = join(Repository.COMMIT_DIR, sha);
        writeObject(thisCommit, this);

        // Changes current branch to point at this commit's
        // SHA-1 hash ID.
        String branch = readContentsAsString(Repository.CURR_BRANCH);
        HashMap<String, String> branches = readObject(Repository.BRANCHES, HashMap.class);
        branches.put(branch, sha);
        writeObject(Repository.BRANCHES, branches);
    }

    public String findParentSHA1() {
        String branch = readContentsAsString(Repository.CURR_BRANCH);
        HashMap<String, String> branches = readObject(Repository.BRANCHES, HashMap.class);
        return branches.get(branch);
    }

    public HashMap<String, String> commitBlobs() {
        Commit commit = readObject(join(Repository.COMMIT_DIR, this.parentSHA), Commit.class);
        HashMap<String, String> oldBlobs = commit.getBlobs();
        HashMap<String, String> blobs = new HashMap<String, String>();

        if (oldBlobs != null) {
            writeObject(Repository.TEMP, oldBlobs);
            blobs = readObject(Repository.TEMP, HashMap.class);
        }

        File[] toBeCommitted = Repository.ADD_DIR.listFiles();

        if (toBeCommitted == null) {
            return blobs;
        }

        for (File f : toBeCommitted) {
            String filename = f.getName();
            byte[] fContents = readContents(f);
            String sha = sha1(fContents);
            blobs.put(filename, sha);
            Repository.copyContents(f, join(Repository.BLOB_DIR, sha));
            f.delete();
        }

        HashSet<String> stagedForRemoval = readObject(Repository.REMOVED, HashSet.class);
        stagedForRemoval.clear();
        writeObject(Repository.REMOVED, stagedForRemoval);

        return blobs;
    }
}
