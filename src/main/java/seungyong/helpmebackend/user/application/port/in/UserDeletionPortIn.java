package seungyong.helpmebackend.user.application.port.in;

import seungyong.helpmebackend.user.application.port.in.command.DeleteUserCommand;
import seungyong.helpmebackend.user.application.port.in.result.UserDeletionResult;

public interface UserDeletionPortIn {
    UserDeletionResult requestDeletion(DeleteUserCommand command);

    void processNext();
}
