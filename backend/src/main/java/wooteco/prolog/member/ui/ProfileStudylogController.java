package wooteco.prolog.member.ui;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import wooteco.prolog.member.application.MemberService;
import wooteco.prolog.member.application.dto.MemberResponse;
import wooteco.prolog.studylog.application.StudylogService;
import wooteco.prolog.studylog.application.dto.StudylogsResponse;

@RestController
@AllArgsConstructor
@RequestMapping("/members")
public class ProfileStudylogController {

    private StudylogService studylogService;
    private MemberService memberService;

    @GetMapping(value = "/{username}/posts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudylogsResponse> findAllStudylogsOfMine(@PathVariable String username,
                                                            @PageableDefault(size = 20, direction = Direction.DESC, sort = "id") Pageable pageable) {
        StudylogsResponse studylogs = studylogService.findStudylogsOf(username, pageable);
        return ResponseEntity.ok().body(studylogs);
    }

    @GetMapping(value = "/{username}/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MemberResponse> findMemberProfile(@PathVariable String username) {
        MemberResponse member = memberService.findMemberResponseByUsername(username);
        return ResponseEntity.ok().body(member);
    }
}