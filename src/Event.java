public abstract class Event {
    protected static final String DELIMITER = "~";

    // Concrete subclasses are expected to provide a constructor that only takes a String object. This String should
    // match the format of those outputted by its toString() method, such that toString() can serialize the object
    // and the String constructor can deserialize the string.

    /**
     * Serializes the object into a string, separated by the DELIMITER.
     *
     * @return String representation of the Event object.
     */
    public abstract String toString();
}
