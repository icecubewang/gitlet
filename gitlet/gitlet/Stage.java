package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Stage implements Serializable {

    private HashMap<String, String> stagedAdded;
    private HashMap<String, String> stagedRemoved;
    private HashMap<String, String> trackedModified;
    private HashMap<String, String> trackedDeleted;
    private HashMap<String, String> untracked;
    private HashMap<String, String> currBlobs;

    //**************************************************************** Public
    //**************************************************************** Methods

    /**
     * Constructor method.
     */
    Stage() {
        stagedAdded = new HashMap<>();
        stagedRemoved = new HashMap<>();
        trackedModified = new HashMap<>();
        trackedDeleted = new HashMap<>();
        untracked = new HashMap<>();
    }

    /**
     * Return the Stage's current status
     * with index as: [0] = stagedAdded, [1] = stagedRemoved,
     * [2] = modifiedFiles, [3] = untrackedFiles
     *
     * @return String[] where each index is one of the Hash sets in the order above.
     */
    public String[] status() {
        check();
        String[] toReturn = new String[4];
        for (int i = 0; i < toReturn.length; i++) {
            toReturn[i] = "";
        }
        for (String key : stagedAdded.keySet()) {
            toReturn[0] += key + "\n";
        }
        for (String key : stagedRemoved.keySet()) {
            toReturn[1] += key + "\n";
        }
        for (String key : trackedDeleted.keySet()) {
            toReturn[2] += key + " (deleted)\n";
        }
        for (String key : trackedModified.keySet()) {
            toReturn[2] += key + " (modified)\n";
        }
        for (String key : untracked.keySet()) {
            toReturn[3] += key + "\n";
        }
        return toReturn;
    }

    /**
     * Check if blob object exists and has or not been modified.
     * Then creates or just adds the blob id and filename to the appropriate state.
     * Updates the state of the file by calling changeFileStatePostAdd.
     * If file does not exist prints error message.
     *
     * @param fileName
     */
    public void add(String fileName, Commit currCommit) {
        String id;
        Boolean fileModified = false;

        if (!stagedRemoved.containsKey(fileName)) {

            String givenBlobContent = Main.readCWDFileToString("", fileName);
            //setCurrentBlobs(currCommit);

            if (currBlobs.containsKey(fileName)) {
                fileModified = hasChanges(fileName, givenBlobContent, true);
                if (!fileModified) {
                    return;
                }
            }

            if (fileModified || !currBlobs.containsKey(fileName)) {
                Blob temp = new Blob(fileName, givenBlobContent);
                Main.writeFile(".temp_blobs", temp.getId(), temp);
                id = temp.getId();
            } else {
                id = currBlobs.get(fileName);
            }

        } else {
            Main.writeToCWDFile("", fileName, getCurrBlobContent(fileName, true));
            id = currBlobs.get(fileName);
        }

        changeFileStatePostAdd(fileName, id);

    }

    /**
     * Removes file
     * If file is tracked and (or not) stages, add it to stagedRemove
     * If file is untracked, then add it to untracked.
     * if file is neither staged nor tracked print error message.
     *
     * @param fileName
     */
    public void remove(String fileName, Commit currCommit) {

        //setCurrentBlobs(currCommit);
        Boolean tracked = currBlobs.containsKey(fileName),
                staged = stagedAdded.containsKey(fileName) || stagedRemoved.containsKey(fileName);

        if (tracked) {
            stagedAdded.remove(fileName);
            trackedModified.remove(fileName);
            trackedDeleted.remove(fileName);
            Main.deleteCWDFile("", fileName);
            Main.deleteFile(".temp_blobs", fileName);
            stagedRemoved.put(fileName, currBlobs.get(fileName));
        } else if (staged) {
            stagedAdded.remove(fileName);
            if (removedFromCWD(fileName)) {
                stagedRemoved.put(fileName, stagedAdded.get(fileName));
            } else {
                untracked.put(fileName, stagedAdded.get(fileName));
            }
        } else {
            System.out.print("No reason to remove the file.");
        }
    }

    /**
     * Sets all of the instance variables back to their default states.
     */
    public void clear() {
        stagedAdded.clear();
        stagedRemoved.clear();
        trackedModified.clear();
        trackedDeleted.clear();
    }

    /**
     * Getter for stagedAdded files
     *
     * @return HashMap with k=fileName and v=id
     */
    public HashMap<String, String> getStagedAdded() {
        return stagedAdded;
    }

    /**
     * Getter for getUntracked files
     *
     * @return HashMap with k=fileName and v=id
     */
    public HashMap<String, String> getUntracked() {
        return untracked;
    }

    /**
     * Getter for removed blobs
     *
     * @return HashMap with k=fileName and v=id
     */
    public HashMap<String, String> getStagedRemoved() {
        return stagedRemoved;
    }

    /**
     * Sets the currentBlobs to the most recent commit's blobs
     *
     * @param currCommit
     */
    public void setCurrentBlobs(Commit currCommit) {
        currBlobs = currCommit.getBlobs();
    }


    /**
     * check if anything has changed in the current directory and
     * updates the instance variables: trackedModified, trackedDeleted, and untracked
     */
    public void check() {
        if (currBlobs != null) {

            ArrayList<String> cwdFiles = Main.getCWDFileNames("");
            String cwdBlobContent;


            for (String fileName : cwdFiles) {
                cwdBlobContent = Main.readCWDFileToString("", fileName);

                if (currBlobs.containsKey(fileName)
                        && hasChanges(fileName, cwdBlobContent, true)) {
                    trackedModified.put(fileName, null);
                } else if ((!currBlobs.containsKey(fileName) // untracked
                        && stagedAdded.containsKey(fileName) // staged
                        && hasChanges(fileName, cwdBlobContent, false)) //modified
                        || (!currBlobs.containsKey(fileName) // untracked
                        && !stagedAdded.containsKey(fileName))) { //not stated
                    untracked.put(fileName, null);
                }
            }

            for (String file : untracked.keySet()) {
                if (!cwdFiles.contains(file)) {
                    untracked.remove(file);
                }
            }

            for (String key : currBlobs.keySet()) {
                if (!cwdFiles.contains(key)) {
                    stagedRemoved.put(key, currBlobs.get(key));
                }
            }

            // if tracked
            // if staged and modified -> add to trackedModified;
            // if deleted -> add to trackedDeleted;
            // shows up in staged, and in modified, and cannot add modified file cannot be added
            // if untracked
            // if not staged -> add to untracked.
            // Tshows up in staged and in modified, and can add new modified file.

        }
    }

    public void updatePostMerge(HashMap<String, String> newBlobs,
                                HashMap<String, String> removedBlobs,
                                HashMap<String, String> conflictFiles) {

        for (String key : newBlobs.keySet()) {
            add(key, Main.mainTree.getCurrentBranchCommit());
//            Object givenBlob = Main.readFile(".blobs", newBlobs.get(key));
//            Blob temp = new Blob(key, ((Blob) givenBlob).getContent());
//            Main.writeFile(".temp_blobs", temp.getId(), temp);
//
//            String id = temp.getId();
//
//            untracked.remove(key, null);
//            stagedAdded.put(key, id);
        }

        for (String key : removedBlobs.keySet()) {
            stagedRemoved.put(key, removedBlobs.get(key));
        }

        for (String key : conflictFiles.keySet()) {
            trackedModified.put(key, null);
        }
    }

    //**************************************************************** Private
    //**************************************************************** Methods

    /**
     * @param fileName
     * @return true if cwd contains file, false otherwise
     */
    private boolean removedFromCWD(String fileName) {
        ArrayList<String> cwdFiles = Main.getCWDFileNames("");
        return !cwdFiles.contains(fileName);
    }

    /**
     * Changes the state of the file and updates the instance variables.
     * If the file is tracked as deleted, then it must be staged to be removed.
     * If the file is tracked as modified, then it must be staged as added.
     * If the file is staged as removed, then it must be added to staged as added.
     * If the file is untracked, then it must be added to staged as added.
     *
     * @param fileName of file added
     * @param id       of file added
     */
    private void changeFileStatePostAdd(String fileName, String id) {
        if (trackedDeleted.containsKey(fileName)) {
            trackedDeleted.remove(fileName);
            stagedRemoved.put(fileName, id);
        } else if (trackedModified.containsKey(fileName)) {
            trackedModified.remove(fileName);
            stagedAdded.put(fileName, id);
        } else if (stagedRemoved.containsKey(fileName)) {
            stagedRemoved.remove(fileName);
        } else {
            untracked.remove(fileName);
            stagedAdded.put(fileName, id);
        }
    }

    /**
     * Checks if there are changes between a given file in CWD and the previous commit.
     *
     * @param fileName
     * @param newBlobContent
     * @return true if the file has been modified, and false otherwise
     */
    private boolean hasChanges(String fileName, String newBlobContent, Boolean tracked) {
        String existingBlobContent = getCurrBlobContent(fileName, tracked);
        return !(newBlobContent.equals(existingBlobContent));
    }

    /**
     * Getter for the content form a blob in the previous commit
     *
     * @param fileName
     * @return String content for that blob
     */
    private String getCurrBlobContent(String fileName, Boolean tracked) {
        Object currBlob;
        if (tracked) {
            currBlob = Main.readFile(".blobs", currBlobs.get(fileName));
        } else {
            currBlob = Main.readFile(".temp_blobs", stagedAdded.get(fileName));
        }
        return ((Blob) currBlob).getContent();
    }

}
