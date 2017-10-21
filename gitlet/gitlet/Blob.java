package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {

    private String name;
    private String id;
    private String content;

    /**
     * Constructor that sets the name, and finds the content.
     * @param givenName
     */
    Blob(String givenName, String givenContent) {
        name = givenName;
        content = givenContent;
        String idtext = "Blobs" + givenName + givenContent;
        id = Utils.sha1(idtext);
    }


    /**
     * Gets the blobs id
     * @return String id
     */
    public String getId() {
        return this.id;
    }

    /**
     * Gets the blobs content
     * @return String content
     */
    public String getContent() {
        return this.content;
    }

    /**
     * Gets the blobs name
     * @return String name
     */
    public String getName() {
        return this.name;
    }
}
