public abstract class Event {
    protected static final String DELIMITER = "~";

    // Concrete subclasses are expected to provide a constructor that only takes a String object. This String should
    // match the format of those outputted by its serialize() method, such that serialize() can serialize the object
    // (i.e., Event -> String) and the String constructor can deserialize the String (i.e., String -> Event).

    /**
     * Serializes the object into a string, separated by the DELIMITER.
     *
     * @return String representation of the Event object.
     */
    public abstract String serialize();
}
