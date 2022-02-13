package gitlet;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Nathan Venier
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    /**
     * Runs one of the following commands:
     *
     * java gitlet.Main init
     * ---  Creates a new Gitlet version-control system in the
     *      current directory. This system will automatically
     *      start with one commit: a commit that contains no
     *      files and has the commit message initial commit
     *      (just like that, with no punctuation). It will have
     *      a single branch: master, which initially points to
     *      this initial commit, and master will be the current
     *      branch. The timestamp for this initial commit will
     *      be 00:00:00 UTC, Thursday, 1 January 1970 in whatever
     *      format you choose for dates (this is called “The
     *      (Unix) Epoch”, represented internally by the time 0.)
     *      Since the initial commit in all repositories created
     *      by Gitlet will have exactly the same content, it
     *      follows that all repositories will automatically share
     *      this commit (they will all have the same UID) and all
     *      commits in all repositories will trace back to it.
     *
     * java gitlet.Main add [file name]
     * ---  Adds a copy of the file as it currently exists to the
     *      staging area (see the description of the commit command).
     *      For this reason, adding a file is also called staging the
     *      file for addition. Staging an already-staged file overwrites
     *      the previous entry in the staging area with the new contents.
     *      The staging area should be somewhere in .gitlet. If the
     *      current working version of the file is identical to the
     *      version in the current commit, do not stage it to be added,
     *      and remove it from the staging area if it is already there
     *      (as can happen when a file is changed, added, and then changed
     *      back to it’s original version). The file will no longer be
     *      staged for removal (see gitlet rm), if it was at the time of
     *      the command.
     *
     * java gitlet.Main commit [message]
     * ---  Saves a snapshot of tracked files in the current commit and
     *      staging area so they can be restored at a later time,
     *      creating a new commit. The commit is said to be tracking the
     *      saved files. By default, each commit’s snapshot of files will
     *      be exactly the same as its parent commit’s snapshot of files;
     *      it will keep versions of files exactly as they are, and not
     *      update them. A commit will only update the contents of files it
     *      is tracking that have been staged for addition at the time of
     *      commit, in which case the commit will now include the version of
     *      the file that was staged instead of the version it got from its
     *      parent. A commit will save and start tracking any files that were
     *      staged for addition but weren’t tracked by its parent. Finally,
     *      files tracked in the current commit may be untracked in the new
     *      commit as a result being staged for removal by the rm command (below).
     *      The bottom line: By default a commit has the same file contents as
     *      its parent. Files staged for addition and removal are the updates to
     *      the commit. Of course, the date (and likely the msesage) will also be
     *      different from the parent.
     *      ~~~ The staging area is cleared after a commit.
     *      ~~~ The commit command never adds, changes, or removes files in the
     *          working directory (other than those in the .gitlet directory). The
     *          rm command will remove such files, as well as staging them for
     *          removal, so that they will be untracked after a commit.
     *      ~~~ Any changes made to files after staging for addition or removal are
     *          ignored by the commit command, which only modifies the contents of
     *          the .gitlet directory. For example, if you remove a tracked file
     *          using the Unix rm command (rather than Gitlet’s command of the same
     *          name), it has no effect on the next commit, which will still contain
     *          the (now deleted) version of the file.
     *      ~~~ After the commit command, the new commit is added as a new node in
     *          the commit tree.
     *      ~~~ The commit just made becomes the “current commit”, and the head
     *          pointer now points to it. The previous head commit is this commit’s
     *          parent commit.
     *      ~~~ Each commit should contain the date and time it was made.
     *      ~~~ Each commit has a log message associated with it that describes the
     *          changes to the files in the commit. This is specified by the user.
     *          The entire message should take up only one entry in the array args
     *          that is passed to main. To include multiword messages, you’ll have
     *          to surround them in quotes.
     *      ~~~ Each commit is identified by its SHA-1 id, which must include the
     *          file (blob) references of its files, parent reference, log message,
     *          and commit time.
     *
     * java gitlet.Main checkout -- [file name]
     * ---  Takes the version of the file as it exists in the head commit and puts
     *      it in the working directory, overwriting the version of the file that’s
     *      already there if there is one. The new version of the file is not staged.
     *
     * java gitlet.Main checkout [commit id] -- [file name]
     * ---  Takes the version of the file as it exists in the commit with the given
     *      id, and puts it in the working directory, overwriting the version of the
     *      file that’s already there if there is one. The new version of the file is
     *      not staged.
     *
     * java gitlet.Main checkout [branch name]
     * ---
     *
     *
     *
     * All persistent data should be stored in a ".gitlet"
     * directory in the current working directory.
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
     *
     * @param args arguments from the command line
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validateNumArgs("init", args, 1);
                initCommand();
                break;
            case "add":
                notInitialized();
                validateNumArgs("add", args, 2);
                addCommand(args[1]);
                break;
            case "commit":
                notInitialized();
                validateNumArgs("commit", args, 2);
                if (args[1].length() == 0) {
                    System.out.println("Please enter a commit message.");
                    System.exit(0);
                }
                commitCommand(args[1]);
                break;
            case "checkout":
                notInitialized();
                if (args.length == 2) {
                    checkoutCommandBranch(args[1]);
                } else if (args.length == 3) {
                    if (!args[1].equals("--")){
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    checkoutCommandHead(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        System.out.println("Incorrect operands.");
                        System.exit(0);
                    }
                    checkoutCommandCommit(args[1], args[3]);
                } else {
                    validateNumArgs("checkout", args, 0);
                }
                break;
            case "log":
                notInitialized();
                validateNumArgs("log", args, 1);
                logCommand();
                break;
            case "rm":
                notInitialized();
                validateNumArgs("rm", args, 2);
                removeCommand(args[1]);
                break;
            case "global-log":
                notInitialized();
                validateNumArgs("global-log", args, 1);
                globalLogCommand();
                break;
            case "find":
                notInitialized();
                validateNumArgs("find", args, 2);
                findCommand(args[1]);
                break;
            case "status":
                notInitialized();
                validateNumArgs("status", args, 1);
                statusCommand();
                break;
            case "branch":
                notInitialized();
                validateNumArgs("branch", args, 2);
                branchCommand(args[1]);
                break;
            case "rm-branch":
                notInitialized();
                validateNumArgs("rm-branch", args, 2);
                removeBranchCommand(args[1]);
                break;
            case "reset":
                notInitialized();
                validateNumArgs("reset", args, 2);
                resetCommand(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
                System.exit(0);
        }
        return;
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }

    public static void notInitialized() {
        if (!Repository.GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }
}
