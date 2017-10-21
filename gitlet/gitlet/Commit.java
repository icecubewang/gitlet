package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.util.Iterator;


public class Commit implements Serializable, Iterable<Commit> {

    /**
     * private variables
     */
    private String id;           //make use of SHA1
    private String parent;       //point to the previous commit
    private String timestamp;    //string of integers "20170715"
    private String message;

    private HashMap<String, String> blobs;


    /**
     * Constructors
     */

    //Default Constructor
    Commit() {
        this.id = "";
        this.parent = "";
        this.timestamp = "";
        this.message = "";
        this.blobs = null;

    }

    //Constructor with arguments
    Commit(String givenParent, String givenMessage, HashMap<String, String> addedFiles,
           HashMap<String, String> removedFiles) {
        this.parent = givenParent;
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.message = givenMessage;
        //create a blobsMap with added&removed

        //if initial commit and no parent just skipp reading
        if (!givenParent.equals("")) {
            blobs = (HashMap) ((Commit) Main.readFile(".commits", getParent())).getBlobs().clone();
        } else {
            this.blobs = new HashMap<>();
        }


        //add files
        for (String key : addedFiles.keySet()) {
            blobs.put(key, addedFiles.get(key));
        }
        //remove files
        for (String key : removedFiles.keySet()) {
            try {

                blobs.remove(key);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }


        //setID
        String idtext = "Commit";
        for (String key : blobs.keySet()) {
            idtext += key;
        }
        idtext += givenParent;
        idtext += givenMessage;

        id = Utils.sha1(idtext);


    }

    /**
     * All Get Functions
     */
    public String getId() {
        return this.id;
    }

    public String getParent() {
        return this.parent;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public String getMessage() {
        return this.message;
    }

    public HashMap<String, String> getBlobs() {
        return this.blobs;
    }

    /**
     *Commit Methods
     */

    /**
     * Find the split point of two branches [this is a method to be used by merge()]
     * @param branch
     * @return the Commit node which is the split point
     */
    /**
     * Commit Methods
     */

    public Commit findSplitPoint(Commit branch) {
        Commit aLoopThrough = this;
        Commit bLoopThrough = branch;
        while (aLoopThrough == null || !aLoopThrough.equals(bLoopThrough)) {
            //aLoopThrough move one step forward
            if (aLoopThrough != null) {
                aLoopThrough = (Commit) Main.readFile(".commits", aLoopThrough.getParent());
            } else {
                aLoopThrough = branch;
            }
            //bLoopThrough move one step forward
            if (bLoopThrough != null) {
                bLoopThrough = (Commit) Main.readFile(".commits", bLoopThrough.getParent());
            } else {
                bLoopThrough = this;
            }
        }
        return aLoopThrough;
    }


    /**
     * Compares two Commit objects by their ID
     * If pass in a non-Commit object, throws an Exception
     *
     * @param o
     * @return true if equals, false elsewise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Commit commit = (Commit) o;

        return id != null ? id.equals(commit.id) : commit.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    /**
     * Commit Iterator
     */
    public Iterator<Commit> iterator() {
        return new CommitIterator();
    }

    private class CommitIterator implements Iterator<Commit> {
        Commit temp = (Commit) Main.readFile(".commits", getId());

        @Override
        public boolean hasNext() {
            return temp.getParent().equals("") ? false : true;
        }

        @Override
        public Commit next() {
            temp = (Commit) Main.readFile(".commits", temp.getParent());
            return temp;

        }
    }

}
