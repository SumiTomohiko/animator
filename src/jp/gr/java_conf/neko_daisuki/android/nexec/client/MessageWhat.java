package jp.gr.java_conf.neko_daisuki.android.nexec.client;

public interface MessageWhat {

    /**
     * The message type from a client to a service to request to send the
     * current status. Response type of this message type are
     * <code>STDOUT</code>, <code>STDERR</code>, <code>FINISHED</code> or
     * nothing.
     */
    public int TELL_STATUS = 42;

    /**
     * The message type from a service to a client to send stdout.
     */
    public int STDOUT = 43;

    /**
     * The message type from a service to a client to send stderr.
     */
    public int STDERR = 44;

    /**
     * The message type from a service to a client to tell the task was
     * finished.
     */
    public int FINISHED = 45;
}

// vim: tabstop=4 shiftwidth=4 expandtab softtabstop=4
