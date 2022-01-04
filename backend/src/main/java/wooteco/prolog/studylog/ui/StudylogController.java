package wooteco.prolog.studylog.ui;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wooteco.prolog.login.aop.MemberOnly;
import wooteco.prolog.login.domain.AuthMemberPrincipal;
import wooteco.prolog.login.ui.LoginMember;
import wooteco.prolog.studylog.application.StudylogService;
import wooteco.prolog.studylog.application.dto.StudylogRequest;
import wooteco.prolog.studylog.application.dto.StudylogResponse;
import wooteco.prolog.studylog.application.dto.StudylogsResponse;
import wooteco.prolog.studylog.application.dto.search.SearchParams;
import wooteco.prolog.studylog.application.dto.search.StudylogsSearchRequest;
import wooteco.prolog.studylog.exception.StudylogNotFoundException;
import wooteco.support.number.NumberUtils;

@RestController
@RequestMapping("/studylogs")
public class StudylogController {

    private final StudylogService studylogService;

    public StudylogController(StudylogService studylogService) {
        this.studylogService = studylogService;
    }

    @GetMapping
    public ResponseEntity<StudylogsResponse> showAll(
        @AuthMemberPrincipal LoginMember member,
        @SearchParams StudylogsSearchRequest searchRequest) {
        StudylogsResponse studylogsResponse = studylogService.findStudylogs(searchRequest, member.getId(), member.isAnonymous());
        return ResponseEntity.ok(studylogsResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudylogResponse> showStudylog(
        @PathVariable String id,
        @AuthMemberPrincipal LoginMember member
    ) {
        if (!NumberUtils.isNumeric(id)) {
            throw new StudylogNotFoundException();
        }
        StudylogResponse studylogResponse = studylogService.findById(Long.parseLong(id), member.getId(),
            member.isAnonymous());
        return ResponseEntity.ok(studylogResponse);
    }
}
