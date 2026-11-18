import java.io.IOException;
import java.util.*;

// Unchecked domain exceptions (caller/input/state violations)
class ExamPortalRuntimeException extends RuntimeException {
    ExamPortalRuntimeException(String msg) { super(msg); }
}
class ExamWindowClosedException extends ExamPortalRuntimeException {
    ExamWindowClosedException(String msg) { super(msg); }
}
class StudentNotRegisteredException extends ExamPortalRuntimeException {
    StudentNotRegisteredException(String msg) { super(msg); }
}

// Storage Layer
class PortalStorageLayer {
    void persistFile(String studentId, String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            if (sizeKb > 500) {
                throw new IOException("Disk quota exceeded (" + sizeKb + " KB > 500 KB limit)");
            }
            System.out.println("[Storage] Persisted " + fileName + " for " + studentId);
        } catch (IOException ioe) {
            throw new SubmissionStorageException("Storage failure while saving " + fileName, ioe);
        }
    }
}

// Service Layer
class ExamSubmissionService {
    private final PortalStorageLayer storage = new PortalStorageLayer();
    private final Set<String> registeredStudents = new HashSet<>(Arrays.asList("23CS101", "23CS102"));

    void submitAnswer(String studentId, boolean windowOpen, String fileName, long sizeKb)
            throws SubmissionStorageException {
        if (!windowOpen) {
            throw new ExamWindowClosedException("Submission rejected: Exam window has closed.");
        }
        if (!registeredStudents.contains(studentId)) {
            throw new StudentNotRegisteredException("Submission rejected: Student " + studentId + " is not registered.");
        }
        storage.persistFile(studentId, fileName, sizeKb);
    }
}

// Controller Layer
public class ExamPortalPropagationDemo {
    private final ExamSubmissionService service = new ExamSubmissionService();

    void handleSubmission(String studentId, boolean windowOpen, String fileName, long sizeKb) {
        System.out.println("\n--- Portal Request: student=" + studentId + ", windowOpen=" + windowOpen + ", file=" + fileName + " (" + sizeKb + "KB) ---");
        try {
            service.submitAnswer(studentId, windowOpen, fileName, sizeKb);
            System.out.println("[Controller UI] 200 OK: Submission confirmed.");
        } catch (ExamWindowClosedException | StudentNotRegisteredException e) {
            System.out.println("[Controller UI] 400 Bad Request: " + e.getMessage());
        } catch (SubmissionStorageException e) {
            System.out.println("[Controller UI] 500 Internal Storage Error: " + e.getMessage());
            System.out.println("[Controller Log] Root cause preserved via getCause(): " + e.getCause());
        }
    }

    public static void main(String[] args) {
        ExamPortalPropagationDemo controller = new ExamPortalPropagationDemo();
        controller.handleSubmission("23CS101", true, "script1.pdf", 240);   // Success
        controller.handleSubmission("23CS101", false, "script1.pdf", 240);  // ExamWindowClosedException
        controller.handleSubmission("23CS999", true, "script1.pdf", 240);   // StudentNotRegisteredException
        controller.handleSubmission("23CS102", true, "script2.pdf", 750);   // Chained SubmissionStorageException -> IOException
    }
}
