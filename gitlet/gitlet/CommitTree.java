package gitlet;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;


public class CommitTree implements Serializable {
    /*Holds String branchNameKey, and currentCommitFileId the branch points to*/
    private HashMap<String, String> branches;
    String currentBranchKey;
    static String sysDir = System.getProperty("user.dir");


    public CommitTree() {
        branches = new HashMap<>();
        Commit newCommit = new Commit("", "initial commit", new HashMap<String, String>(),
                new HashMap<String, String>());

        Main.writeFile(".commits", newCommit.getId(), newCommit);
        branches.put("master", newCommit.getId());
        currentBranchKey = "master";

    }


    /**
     * Creates a new commit in commitTree under current branch.
     * But not the commit object only commits file name.
     *
     * @param added   //new added blobs/files to newly created commit
     * @param removed //list of removed files to create commit
     */
    public void addToCommitTree(String message, HashMap<String, String>
            added, HashMap<String, String> removed) {
        Commit newCommit = new Commit(getCurrentBranchCommit().getId(), message, added, removed);
        Main.writeFile(".commits", newCommit.getId(), newCommit);

        //arraylist of temp_blobs
        ArrayList<Object> files = Main.readFiles(".temp_blobs");

        for (Object file : files) {
            Blob tempBlob = (Blob) file;
            //add files into .blobs (permanent blobs dir)
            Main.writeFile(".blobs", tempBlob.getId(), tempBlob);
            //delete files from .temp_blobs because we committed
            Main.deleteFile(".temp_blobs", tempBlob.getId());
        }


        branches.put(currentBranchKey, newCommit.getId());

        Main.mainStage.setCurrentBlobs(getCurrentBranchCommit());
        Main.mainStage.clear();

    }

    /**
     * Returns id for the previous commit for a current branch.
     */
    public String getPreviousCommitId() {
        return getCurrentBranchCommit().getParent();
    }

    /**
     * Starting at the current head commit, display information about each commit backwards
     * along the commit tree until the initial commit. This set of commit nodes is
     * called the commit's history. For every node in this history,
     * the information it should display is the commit id,
     * the time the commit was made, and the commit message.
     */
    public String log() {
        String toReturn = "";
        Commit currentCommit = getCurrentBranchCommit();
        Iterator iter = currentCommit.iterator();

        toReturn += "===\n";
        toReturn += "Commit " + currentCommit.getId() + "\n";
        toReturn += currentCommit.getTimestamp() + "\n";
        toReturn += currentCommit.getMessage() + "\n";
        toReturn += "\n";

        while (iter.hasNext()) {
            Commit curr = (Commit) iter.next();
            toReturn += "===\n";
            toReturn += "Commit " + curr.getId() + "\n";
            toReturn += curr.getTimestamp() + "\n";
            toReturn += curr.getMessage() + "\n";
            toReturn += "\n";
        }

        return toReturn.substring(0, toReturn.length() - 1);
    }


    /**
     * Outputs the history of commits/backups for all branchName. Order does not matter.
     */
    public String globalLog() {
        ArrayList allCommits = Main.readFiles(".commits");
        String toReturn = "";
        for (Object c : allCommits) {
            toReturn += "===\n";
            toReturn += "Commit " + ((Commit) c).getId() + "\n";
            toReturn += ((Commit) c).getTimestamp() + "\n";
            toReturn += ((Commit) c).getMessage() + "\n";
            toReturn += "\n";
        }
        return toReturn.substring(0, toReturn.length() - 1);
    }

    /**
     * Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits,
     * it prints the ids out on separate lines.
     */
    public String find(String commitMessage) {
        Path path = Paths.get(System.getProperty("user.dir"), ".commits");
        ArrayList f = Main.readFiles(".commits");
        String toReturn = "";

        for (Object file : f) {
            Commit tempCommit = (Commit) file;
            if (tempCommit.getMessage().equals(commitMessage)) {
                toReturn += tempCommit.getId() + "\n";
            }
        }
        return toReturn;
    }


    /**
     * Returns string array of branches with current branch with a *
     */
    public String status() {
        String toReturn = "";

        for (String key : branches.keySet()) {
            if (key.equals("master")) {
                if (key.equals(currentBranchKey)) {
                    toReturn = "*" + key + "\n" + toReturn;
                } else {
                    toReturn = key + "\n" + toReturn;
                }
            } else {
                if (key.equals(currentBranchKey)) {
                    toReturn += "*" + key + "\n";
                } else {
                    toReturn += key + "\n";
                }
            }
        }
        return toReturn;
    }


    /**
     * Creates a new branch with the given name, and points it at the current head node.
     */
    public void branch(String newBranch) {
        if (branches.containsKey(newBranch)) {
            System.out.println("A branch with that name already exists.");
        } else {
            branches.put(newBranch, branches.get(currentBranchKey));
        }
    }

    /**
     * Deletes the branch with the given name.
     * This only means to delete the pointer associated with the branch;
     * it does not mean to delete all commits that were created
     * under the branch, or anything like that.
     */
    public void rmBranch(String branchToRemove) {
        if (!branches.containsKey(branchToRemove)) {
            System.out.println("A branch with that name does not exist.");
        } else if (currentBranchKey.equals(branchToRemove)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branches.remove(branchToRemove);
        }

    }

    public String getAbrevCommitIfExsits(String abrev) {

        Path path = Paths.get(System.getProperty("user.dir"), ".commits");
        ArrayList f = Main.readFiles(".commits");

        for (Object file : f) {
            Commit tempCommit = (Commit) file;
            String tempId = tempCommit.getId();
            if (tempId.substring(0, abrev.length()).equals(abrev)) {
                return ((Commit) file).getId();
            }
        }
        return "";
    }

    /**
     * Resets current branch to point to previous commit.
     * NOTE: Clearing of the stage happens in stage rest.
     * NOTE: Removes the files that were added before resetting (happens inside stage class).
     */
    public void reset(String newCommitToPointTo) {

        newCommitToPointTo = getAbrevCommitIfExsits(newCommitToPointTo);

        boolean checkIfExists = (newCommitToPointTo.equals("")) ? false : true;

        Commit currentCommit = getCurrentBranchCommit();
        Iterator iter = currentCommit.iterator();
        Path path = Paths.get(System.getProperty("user.dir"), ".commits");

        Commit temp = currentCommit;

        if (checkIfExists) {

            //getting commit we want to restore to
            Commit newCommit = (Commit) Main.readFile(".commits", newCommitToPointTo);

            HashMap<String, String> blobsToRestore = newCommit.getBlobs();

            // check if files are untracked
            //TODO can make more efficient
            Main.mainStage.check();
            HashMap<String, String> untrackedFilesInCurrBranch = Main.mainStage.getUntracked();

            if (!untrackedFilesInCurrBranch.isEmpty()) {
                for (String utFile : untrackedFilesInCurrBranch.keySet()) {
                    if (blobsToRestore.keySet().contains(utFile)) {
                        System.out.println("There is an untracked file in the way; "
                                + "delete it or add it first.");
                        return;
                    }
                }
            }

            while (iter.hasNext() && !temp.getId().equals(newCommitToPointTo)) {

                if (!temp.getId().equals(newCommitToPointTo)) {

                    //delete all files associated with that commit
                    HashMap<String, String> blobs = temp.getBlobs();
                    //iterate through each blob and delete corresponding file in CWD
                    for (String fileName : blobs.keySet()) {
                        if (!blobsToRestore.values().contains(blobs.get(fileName))) {
                            Main.deleteCWDFile("", fileName);
                        }
                    }
                }
                temp = (Commit) iter.next();
            }

            //Get needed files to CWD
            for (String file : blobsToRestore.keySet()) {
                Blob tBlob = (Blob) Main.readFile(".blobs", blobsToRestore.get(file));
                Main.writeToCWDFile("", file, tBlob.getContent());
            }

            branches.put(currentBranchKey, newCommitToPointTo);
            Main.mainStage.clear();
            Main.mainStage.setCurrentBlobs(newCommit);
        } else {
            System.out.println("No commit with that id exists.");
        }
    }

    public Commit getCurrentBranchCommit() {
        String currentCommitId = branches.get(currentBranchKey);
        return (Commit) Main.readFile(".commits", currentCommitId);
    }

    /**
     * Merges files from the given branch into the current branch.
     */
    public void merge(String givenBranch, Stage s) {

        //if there are staged additions or removals present ==> print error message
        if (!s.getStagedAdded().isEmpty() || !s.getStagedRemoved().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        //if a branch with the given name does not exist ==> print error message
        if (!branches.containsKey(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        //if attempting to merge a branch with itself ==> print error message
        if (currentBranchKey.equals(givenBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit givenCommit = (Commit) Main.readFile(".commits", branches.get(givenBranch));
        if (getSplitPoint(givenBranch, givenCommit) == null) {
            return;
        }

        Commit splitPoint = getSplitPoint(givenBranch, givenCommit);

        HashMap<String, String> givenBlobs = givenCommit.getBlobs();
        HashMap<String, String> splitPointBlobs = splitPoint.getBlobs();
        HashMap<String, String> currentBlobs = getCurrentBranchCommit().getBlobs();
        HashMap<String, String> removedBlobs = new HashMap<>();
        HashMap<String, String> newBlobs = new HashMap<>();
        HashMap<String, String> conflictFiles = new HashMap<>(); // key=filename, value=file content


        Main.writeFile("", ".temp_stage", Main.mainStage);
        Main.mainStage.check();
        updateCwdAndStage(givenBlobs, splitPointBlobs, currentBlobs,
                removedBlobs, newBlobs, conflictFiles, givenCommit);


        HashMap<String, String> untrackedFilesInCurrBranch = Main.mainStage.getUntracked();

        if (!untrackedFilesInCurrBranch.isEmpty()) {
            for (String utFile : untrackedFilesInCurrBranch.keySet()) {
                if (newBlobs.keySet().contains(utFile) || removedBlobs.keySet().contains(utFile)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");

                    //merge failed -> restore the stage
                    Main.mainStage = (Stage) Main.readFile("", ".temp_stage");
                    //delete temp stage from temp_stage file after restore
                    Main.deleteFile(sysDir, ".temp_stage");
                    return;
                }
            }
        }

        //DELETE FILES ONLY WHEN THE MERGE IS APPROVED
        for (String key : currentBlobs.keySet()) {
            //present at splitPoint, unmodified in current, absent in given
            if (splitPointBlobs.containsKey(key)
                    && !givenBlobs.containsKey(key)) {
                if (currentBlobs.get(key).equals(splitPointBlobs.get(key))) {
                    Main.deleteCWDFile("", key);
                }
            }
        }
        Main.mainStage.updatePostMerge(newBlobs, removedBlobs, conflictFiles);

        if (conflictFiles.isEmpty()) {
            String commitMessage = "Merged " + currentBranchKey + " with " + givenBranch + ".";
            addToCommitTree(commitMessage, newBlobs, removedBlobs);

        } else if (!conflictFiles.isEmpty()) {
            ArrayList<String> cwdFiles = Main.getCWDFileNames("");

            for (String fileName : cwdFiles) {
                if (conflictFiles.containsKey(fileName)) {
                    Main.writeToCWDFile("", fileName, conflictFiles.get(fileName));
                }
            }

            Main.mainStage.clear();
            Main.mainStage.updatePostMerge(newBlobs, removedBlobs, conflictFiles);
            System.out.println("Encountered a merge conflict.");
        }


    }

    private void updateCwdAndStage(HashMap<String, String> givenBlobs,
                                   HashMap<String, String> splitPointBlobs,
                                   HashMap<String, String> currentBlobs,
                                   HashMap<String, String> removedBlobs,
                                   HashMap<String, String> newBlobs,
                                   HashMap<String, String> conflictFiles,
                                   Commit givenCommit) {

        for (String key : givenBlobs.keySet()) { // when present in given blobs
            //given branch is modified & splitPoint present & current branch unmodified
            if (currentBlobs.containsKey(key) && splitPointBlobs.containsKey(key)) {
                if ((currentBlobs.get(key).equals(splitPointBlobs.get(key)))
                        && !(givenBlobs.get(key).equals(splitPointBlobs.get(key)))) {
                    newBlobs.put(key, givenBlobs.get(key));
                } else if (!currentBlobs.get(key).equals(splitPointBlobs.get(key))
                        && (givenBlobs.get(key).equals(splitPointBlobs.get(key)))) {
                    newBlobs.put(key, splitPointBlobs.get(key));
                }
                //given branch present & splitPoint absent & current branch absent
            }
            if ((!currentBlobs.containsKey(key))
                    && !(splitPointBlobs.containsKey(key))) {
                checkOutCommit(givenCommit.getId(), key);
                newBlobs.put(key, givenBlobs.get(key));
            }
            if (currentBlobs.containsKey(key) && splitPointBlobs.containsKey(key)) {
                // CONFLICT all have file, all different
                if (!givenBlobs.get(key).equals(splitPointBlobs.get(key))
                        && !currentBlobs.get(key).equals(splitPointBlobs.get(key))
                        && !currentBlobs.get(key).equals(givenBlobs.get(key))) {
                    writeConflictFile(currentBlobs, givenBlobs, conflictFiles, key);
                }
                // CONFLICT absent in splitpoint, given and current not equal.
            }
            if (currentBlobs.containsKey(key) && !splitPointBlobs.containsKey(key)) {
                if (!givenBlobs.get(key).equals(currentBlobs.get(key))) {
                    writeConflictFile(currentBlobs, givenBlobs, conflictFiles, key);
                }
                // CONFLICT absent in current, modified in given
            }
            //CONFLICT absent in current, modified in given, present in splitpoint
            if (!currentBlobs.containsKey(key)
                    && splitPointBlobs.containsKey(key)
                    && givenBlobs.containsKey(key)) {
                if (!givenBlobs.get(key).equals(splitPointBlobs.get(key))) {
                    writeConflictFile(currentBlobs, givenBlobs, conflictFiles, key);
                }
            }
        }

        // when absent in given && present in current Blobs
        for (String key : currentBlobs.keySet()) {
            if (!splitPointBlobs.containsKey(key)
                    && !givenBlobs.containsKey(key)) { // only present in current
                newBlobs.put(key, currentBlobs.get(key));

                //present at splitPoint, unmodified in current, absent in given
            }
            if (splitPointBlobs.containsKey(key)
                    && !givenBlobs.containsKey(key)) {
                if (currentBlobs.get(key).equals(splitPointBlobs.get(key))) {
                    //Main.deleteCWDFile("", key);
                    removedBlobs.put(key, givenBlobs.get(key));
                }
            }
            //present at splitpoint, modified in the current, deleted in given ==> conflict
            if (splitPointBlobs.containsKey(key)
                    && currentBlobs.containsKey(key)
                    && !givenBlobs.containsKey(key)) {
                if (!splitPointBlobs.get(key).equals(currentBlobs.get(key))) {
                    writeConflictFile(currentBlobs, givenBlobs, conflictFiles, key);
                }
            }
        }

    }

    /**
     * Write ConflictFile (to be used in merge)
     *
     * @param currentBlobs
     * @param givenBlobs
     * @param conflictFiles
     * @param key
     */
    private void writeConflictFile(HashMap currentBlobs,
                                   HashMap givenBlobs, HashMap conflictFiles, String key) {
        String message = "<<<<<<< HEAD" + "\n";
        if (!currentBlobs.containsKey(key)) {
            message += "";
        } else {
            Blob curr = (Blob) Main.readFile(".blobs", (String) currentBlobs.get(key));
            message += curr.getContent();
        }
        message += "=======" + "\n";
        if (!givenBlobs.containsKey(key)) {
            message += "";
        } else {
            Blob given = (Blob) Main.readFile(".blobs", (String) givenBlobs.get(key));
            message += given.getContent();
        }
        message += ">>>>>>>" + "\n";
        conflictFiles.put(key, message);
    }


    /**
     * Get the split point between the head of given branch and a certain commit.
     *
     * @param givenBranch
     * @param givenCommit
     * @return the splitPoint commit
     */
    private Commit getSplitPoint(String givenBranch, Commit givenCommit) {
        Commit splitPoint = getCurrentBranchCommit().findSplitPoint(givenCommit);
        //if the split point is same as given ==> do nothing
        if (splitPoint.equals(givenCommit)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return null;
        }
        //if the split point is the same as current ==> change current to given
        if (splitPoint.equals(getCurrentBranchCommit())) {
            currentBranchKey = givenBranch;
            System.out.println("Current branch fast-forwarded.");
            return null;
        }
        //if no common ancestor ==> initial commit is the split point
        if (splitPoint == null) {
            Commit initialCommit = getCurrentBranchCommit();
            while (initialCommit.getParent() != null) {
                initialCommit = (Commit) Main.readFile(".commits", initialCommit.getParent());
            }
            splitPoint = initialCommit;
        }
        return splitPoint;
    }


    public void checkOutFile(String filename) {
        //go to the head commit
        Commit initialCommit = getCurrentBranchCommit();
        HashMap<String, String> currBlobs = initialCommit.getBlobs();

        if (currBlobs.containsKey(filename)) {
            Blob add = (Blob) Main.readFile(".blobs", currBlobs.get(filename));
            Main.writeToCWDFile("", filename, add.getContent());
        } else {
            System.out.println("File does not exist in that commit.");
        }

    }

    public void checkOutCommit(String commitID, String filename) {

        // resolve abbreviated commit ID
        commitID = getAbrevCommitIfExsits(commitID);

        if (commitID.equals("")) {
            System.out.println("No commit with that id exists.");
            return;
        }

        Commit wantedCommit = (Commit) Main.readFile(".commits", commitID);
        HashMap<String, String> currBlobs = wantedCommit.getBlobs();
        if (currBlobs.containsKey(filename)) {
            Blob add = (Blob) Main.readFile(".blobs", currBlobs.get(filename));
            Main.writeToCWDFile("", filename, add.getContent());
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    public void checkOutBranch(String branchName) {
        //Check for failure cases
        if (!branches.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (branchName.equals(currentBranchKey)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        Commit givenCommit = (Commit) Main.readFile(".commits", branches.get(branchName));
        HashMap<String, String> givenBlobs = givenCommit.getBlobs();

        ArrayList<String> filesInCurrBlobs = new ArrayList<>(givenBlobs.keySet());

        Main.mainStage.check();

        // check if files are untracked
        //TODO can make more efficient

        HashMap<String, String> untrackedFilesInCurrBranch = Main.mainStage.getUntracked();

        if (!untrackedFilesInCurrBranch.isEmpty()) {
            for (String utFile : untrackedFilesInCurrBranch.keySet()) {
                if (givenBlobs.keySet().contains(utFile)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it or add it first.");
                    return;
                }
            }
        }

        //remove tracked files from CWD
        for (String key : getCurrentBranchCommit().getBlobs().keySet()) {
            Main.deleteCWDFile("", key);
        }

        //add to CWD
        for (String key : filesInCurrBlobs) {
            String blobContent = ((Blob) Main.readFile(".blobs",
                    givenBlobs.get(key))).getContent();
            Main.writeToCWDFile("", key, blobContent);
        }
        currentBranchKey = branchName;
        Main.mainStage.setCurrentBlobs(givenCommit);
        Main.mainStage.clear();
    }

}
