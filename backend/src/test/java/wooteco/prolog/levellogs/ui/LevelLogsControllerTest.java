package wooteco.prolog.levellogs.ui;

import static java.util.Comparator.comparing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import wooteco.prolog.levellogs.application.LevelLogService;
import wooteco.prolog.levellogs.application.dto.LevelLogResponse;
import wooteco.prolog.levellogs.application.dto.LevelLogSummaryResponse;
import wooteco.prolog.levellogs.application.dto.LevelLogSummariesResponse;
import wooteco.prolog.levellogs.domain.LevelLog;
import wooteco.prolog.levellogs.domain.SelfDiscussion;
import wooteco.prolog.levellogs.domain.repository.LevelLogRepository;
import wooteco.prolog.levellogs.domain.repository.SelfDiscussionRepository;
import wooteco.prolog.levellogs.exception.LevelLogNotFoundException;
import wooteco.prolog.member.domain.Member;
import wooteco.prolog.member.domain.Role;
import wooteco.prolog.member.domain.repository.MemberRepository;
import wooteco.support.utils.RepositoryTest;

@RepositoryTest
public class LevelLogsControllerTest {

    @Autowired
    LevelLogRepository levelLogRepository;

    @Autowired
    SelfDiscussionRepository selfDiscussionRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("레벨 로그 ID로 상세 정보를 조회한다.")
    void findByLevelLogId() {
        // arrange
        Member author = memberRepository.save(new Member("verus", "verus", Role.CREW, 1L, "image"));
        LevelLog levelLog = levelLogRepository.save(new LevelLog("제목", "내용", author));
        SelfDiscussion discussion1 = selfDiscussionRepository.save(new SelfDiscussion(levelLog, "질문1", "응답1"));
        SelfDiscussion discussion2 = selfDiscussionRepository.save(new SelfDiscussion(levelLog, "질문2", "응답2"));
        SelfDiscussion discussion3 = selfDiscussionRepository.save(new SelfDiscussion(levelLog, "질문3", "응답3"));

        LevelLogsController sut = new LevelLogsController(
                new LevelLogService(levelLogRepository, selfDiscussionRepository));

        // act
        ResponseEntity<LevelLogResponse> response = sut.findById(levelLog.getId());

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isEqualTo(
                        new LevelLogResponse(levelLog, Arrays.asList(discussion1, discussion2, discussion3))
                )
        );
    }

    @Test
    @DisplayName("존재하지 않는 레벨 로그 ID로 상세 정보 조회 시 예외를 반환한다.")
    void findByNotFoundLevelLogId() {
        LevelLogsController sut = new LevelLogsController(
                new LevelLogService(levelLogRepository, selfDiscussionRepository));

        assertThatThrownBy(() -> sut.findById(1L))
                .isInstanceOf(LevelLogNotFoundException.class);
    }

    @DisplayName("레벨 로그 목록을 조회한다.")
    @Test
    void findLevelLogs() {
        // arrange
        Member verus = memberRepository.save(new Member("verus", "verus", Role.CREW, 1L, "verus-image"));
        Member sudal = memberRepository.save(new Member("sudal", "sudal", Role.CREW, 2L, "sudal-image"));

        List<LevelLogSummaryResponse> verusLevelLogs = saveLevelLog(verus, 3);
        List<LevelLogSummaryResponse> sudalLevelLogs = saveLevelLog(sudal, 2);

        LevelLogsController sut = new LevelLogsController(
                new LevelLogService(levelLogRepository, selfDiscussionRepository));

        // act
        ResponseEntity<LevelLogSummariesResponse> response = sut
                .findAll(PageRequest.of(0, 3, Sort.by(Order.desc("createdAt"))));

        // assert
        List<LevelLogSummaryResponse> totalLevelLogs = new ArrayList<>(verusLevelLogs);
        totalLevelLogs.addAll(sudalLevelLogs);

        List<LevelLogSummaryResponse> expectedResponse = totalLevelLogs.stream()
                .sorted(comparing(LevelLogSummaryResponse::getCreatedAt).reversed())
                .collect(Collectors.toList());

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isEqualTo(
                        new LevelLogSummariesResponse(expectedResponse.subList(0, 3), 5L, 2, 1)
                )
        );
    }

    private List<LevelLogSummaryResponse> saveLevelLog(Member member, int count) {
        List<LevelLogSummaryResponse> responses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LevelLog levelLog = levelLogRepository.save(new LevelLog("제목 " + i, "내용 " + i, member));
            responses.add(new LevelLogSummaryResponse(levelLog));
        }
        return responses;
    }
}
