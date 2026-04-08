public class MessageBox {

    private Message message = null;

    private boolean boxFull = false;
    private boolean boxClosed = false;

    private int numWaitingThreads = 0;

    public MessageBox() {
    }

    /**
     * Retrieves the Message object that is inside the message box, emptying the
     * box. If the box is empty, it waits until the box has a message.
     *
     * @return the Message object that was inside the box, or `null` if the box
     * has been closed.
     */
    public synchronized Message getMessage() {
        while (!boxFull) {
            if (boxClosed) {
                return null;
            }

            try {
                numWaitingThreads += 1;

                wait();
            } catch (InterruptedException ie) {
                numWaitingThreads -= 1;

                ie.printStackTrace();
                System.exit(1);
            }
            numWaitingThreads -= 1;
        }

        Message retrievedMessage = message;

        message = null;
        boxFull = false;

        notifyAll();
        return retrievedMessage;
    }

    /**
     * Places a Message object inside the message box, filling the box. If the
     * box is full, it waits until the box has been emptied.
     *
     * @param message the Message object to be placed in the box.
     *
     * @return the Message object that has been placed in the box, or `null` if
     * the box has been closed.
     */
    public synchronized Message putMessage(Message message) {
        while (boxFull || boxClosed) {
            if (boxClosed) {
                return null;
            }

            try {
                numWaitingThreads += 1;

                wait();
            } catch (InterruptedException ie) {
                numWaitingThreads -= 1;

                ie.printStackTrace();
                System.exit(1);
            }
            numWaitingThreads -= 1;
        }

        this.message = message;
        boxFull = true;

        notifyAll();
        return this.message;
    }

    public synchronized boolean isFull() {
        return boxFull;
    }

    /**
     * Closes the message box, causing all future interactions to return without
     * an affect. If there are any threads currently waiting on this object
     * (excluding any calls to this function), then it waits until all awaiting
     * operations have concluded before closing the box.
     *
     */
    public synchronized void closeBox() {
        while (boxFull || boxClosed || (numWaitingThreads > 0)) {
            if (boxClosed) {
                return;
            }

            try {
                wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                System.exit(1);
            }
        }
        notifyAll();

        boxClosed = true;
    }
}
