package seungyong.helpmebackend.adapter.out.persistence.mapper;

import javax.annotation.processing.Generated;
import seungyong.helpmebackend.adapter.out.persistence.entity.UserJpaEntity;
import seungyong.helpmebackend.domain.entity.user.GithubUser;
import seungyong.helpmebackend.domain.entity.user.User;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-26T10:42:25+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.14.3.jar, environment: Java 17.0.3 (Oracle Corporation)"
)
public class UserPortOutMapperImpl implements UserPortOutMapper {

    @Override
    public UserJpaEntity toJpaEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserJpaEntity userJpaEntity = new UserJpaEntity();

        userJpaEntity.setId( user.getId() );
        userJpaEntity.setName( userGithubUserName( user ) );
        userJpaEntity.setGithubId( userGithubUserGithubId( user ) );
        userJpaEntity.setGithubToken( userGithubUserGithubToken( user ) );

        return userJpaEntity;
    }

    @Override
    public User toDomainEntity(UserJpaEntity userJpaEntity) {
        if ( userJpaEntity == null ) {
            return null;
        }

        GithubUser githubUser = null;
        Long id = null;

        githubUser = userJpaEntityToGithubUser( userJpaEntity );
        id = userJpaEntity.getId();

        User user = new User( id, githubUser );

        return user;
    }

    @Override
    public User toDomainEntity(GithubUser githubUser) {
        if ( githubUser == null ) {
            return null;
        }

        GithubUser githubUser1 = null;

        githubUser1 = githubUserToGithubUser( githubUser );

        Long id = null;

        User user = new User( id, githubUser1 );

        return user;
    }

    private String userGithubUserName(User user) {
        if ( user == null ) {
            return null;
        }
        GithubUser githubUser = user.getGithubUser();
        if ( githubUser == null ) {
            return null;
        }
        String name = githubUser.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private Long userGithubUserGithubId(User user) {
        if ( user == null ) {
            return null;
        }
        GithubUser githubUser = user.getGithubUser();
        if ( githubUser == null ) {
            return null;
        }
        Long githubId = githubUser.getGithubId();
        if ( githubId == null ) {
            return null;
        }
        return githubId;
    }

    private String userGithubUserGithubToken(User user) {
        if ( user == null ) {
            return null;
        }
        GithubUser githubUser = user.getGithubUser();
        if ( githubUser == null ) {
            return null;
        }
        String githubToken = githubUser.getGithubToken();
        if ( githubToken == null ) {
            return null;
        }
        return githubToken;
    }

    protected GithubUser userJpaEntityToGithubUser(UserJpaEntity userJpaEntity) {
        if ( userJpaEntity == null ) {
            return null;
        }

        String name = null;
        Long githubId = null;
        String githubToken = null;

        name = userJpaEntity.getName();
        githubId = userJpaEntity.getGithubId();
        githubToken = userJpaEntity.getGithubToken();

        GithubUser githubUser = new GithubUser( name, githubId, githubToken );

        return githubUser;
    }

    protected GithubUser githubUserToGithubUser(GithubUser githubUser) {
        if ( githubUser == null ) {
            return null;
        }

        String name = null;
        Long githubId = null;
        String githubToken = null;

        name = githubUser.getName();
        githubId = githubUser.getGithubId();
        githubToken = githubUser.getGithubToken();

        GithubUser githubUser1 = new GithubUser( name, githubId, githubToken );

        return githubUser1;
    }
}
