package contactapp;

/**
 * Service class for managing contacts.
 * Provides methods to add, delete, and update contacts.
 * Using singleton pattern to ensure a single instance.
 **/

public final class ContactService {

    private static ContactService instance;

    private ContactService() {
        // future state init (in-memory map) will live here
    }

    /**
     * Retrieve the global ContactService instance.
     * @return singleton instance
     */
    public static synchronized ContactService getInstance() {
        if (instance == null) {
            instance = new ContactService();
        }
        return instance;
    }
}
