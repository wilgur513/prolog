package wooteco.prolog.report.application;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wooteco.prolog.login.ui.LoginMember;
import wooteco.prolog.member.domain.Member;
import wooteco.prolog.member.domain.repository.MemberRepository;
import wooteco.prolog.member.exception.MemberNotAllowedException;
import wooteco.prolog.member.exception.MemberNotFoundException;
import wooteco.prolog.report.application.dto.report.ReportAssembler;
import wooteco.prolog.report.application.dto.report.request.ReportRequest;
import wooteco.prolog.report.application.dto.report.request.abilitigraph.AbilityRequest;
import wooteco.prolog.report.application.dto.report.response.ReportResponse;
import wooteco.prolog.report.application.report.ReportsRequestType;
import wooteco.prolog.report.domain.ablity.Ability;
import wooteco.prolog.report.domain.report.Report;
import wooteco.prolog.report.domain.ablity.repository.AbilityRepository;
import wooteco.prolog.report.domain.report.abilitygraph.datastructure.GraphAbilityDto;
import wooteco.prolog.report.domain.report.repository.ReportRepository;
import wooteco.prolog.report.domain.report.studylog.ReportedStudylog;
import wooteco.prolog.report.domain.report.studylog.ReportedStudylogAbility;
import wooteco.prolog.report.exception.GraphAbilitiesAreNotParentException;
import wooteco.prolog.report.exception.ReportNotFoundException;
import wooteco.prolog.report.exception.ReportRequestTypeException;
import wooteco.prolog.report.exception.ReportUpdateException;
import wooteco.prolog.report.exception.UnRelatedAbilityExistenceException;
import wooteco.prolog.studylog.exception.DuplicateReportTitleException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final ReportAssembler reportAssembler;
    private final ReportRepository reportRepository;
    private final AbilityRepository abilityRepository;
    private final MemberRepository memberRepository;
    private final List<ReportsRequestType> reportsRequestTypes;

    @Transactional
    public ReportResponse createReport(ReportRequest reportRequest, LoginMember loginMember) {
        Member member = findMemberById(loginMember.getId());
        Report report = reportAssembler.of(reportRequest, member);

        verifyGraphAbilitiesAreParent(reportRequest);
        verifyStudylogAbilitiesAreChildrenOfGraphAbilities(report);
        verifyDuplicateTitle(report);
        checkIsRepresent(member, report);

        Report savedReport = reportRepository.save(report);

        return reportAssembler.of(savedReport);
    }

    @Transactional
    public ReportResponse updateReport(Long reportId, ReportRequest reportRequest, LoginMember loginMember) {
        try {
            Member member = findMemberById(loginMember.getId());
            Report updateSourceReport = reportAssembler.of(reportRequest, member);

            verifyGraphAbilitiesAreParent(reportRequest);
            verifyStudylogAbilitiesAreChildrenOfGraphAbilities(updateSourceReport);
            verifyDuplicateTitle(updateSourceReport);
            checkIsRepresent(member, updateSourceReport);

            Report savedReport = reportRepository.findById(reportId)
                .orElseThrow(ReportNotFoundException::new);
            verifyIsAllowedUser(member, savedReport);

            savedReport.update(updateSourceReport);

            return reportAssembler.of(savedReport);
        } catch (Exception e) {
            throw new ReportUpdateException();
        }
    }

    private void verifyDuplicateTitle(Report target) {
        reportRepository.findReportByTitleAndMemberUsername(
            target.getTitle(),
            target.getMember().getUsername()
        )
            .filter(report -> !report.getId().equals(target.getId()))
            .ifPresent( r -> {
                throw new DuplicateReportTitleException();
            });
    }

    private void verifyIsAllowedUser(Member member, Report savedReport) {
        if (!Objects.equals(savedReport.getMember(), member)) {
            throw new MemberNotAllowedException();
        }
    }

    private void verifyStudylogAbilitiesAreChildrenOfGraphAbilities(Report report) {
        List<Long> graphAbilityIds = report.getAbilityGraph().getAbilities().stream()
            .filter(GraphAbilityDto::isPresent)
            .map(GraphAbilityDto::getId)
            .collect(toList());

        List<Long> studylogAbilityIds = report.getStudylogs().stream()
            .map(ReportedStudylog::getAbilities)
            .flatMap(Collection::stream)
            .map(ReportedStudylogAbility::getAbility)
            .map(Ability::getId)
            .collect(toList());

        List<Long> childrenAbilityIds = abilityRepository
            .findChildrenAbilitiesByParentId(graphAbilityIds).stream()
            .map(Ability::getId)
            .collect(toList());

        long unrelatedAbilityCnt = studylogAbilityIds.stream()
            .filter(abilityId -> !childrenAbilityIds.contains(abilityId))
            .filter(abilityId -> !graphAbilityIds.contains(abilityId))
            .count();
        
        if(!studylogAbilityIds.isEmpty() && unrelatedAbilityCnt != 0) {
            throw new UnRelatedAbilityExistenceException();
        }
    }

    private void verifyGraphAbilitiesAreParent(ReportRequest reportRequest) {
        List<Long> abilityIds = reportRequest.getAbilityGraph().getAbilities().stream()
            .map(AbilityRequest::getId)
            .distinct()
            .collect(toList());

        Long count = abilityRepository.countParentAbilitiesOf(abilityIds);

        if (count != abilityIds.size()) {
            throw new GraphAbilitiesAreNotParentException();
        }
    }

    private void checkIsRepresent(Member member, Report updateSourceReport) {
        if (updateSourceReport.isRepresent()) {
            reportRepository.findRepresentReportOf(member.getUsername())
                .ifPresent(Report::toUnRepresent);
        }
    }

    public Object findReportsByUsername(String username, String type, Pageable pageable) {
        ReportsRequestType reportsRequest = reportsRequestTypes.stream()
            .filter(reportsRequestType -> reportsRequestType.isSupport(type))
            .findAny()
            .orElseThrow(ReportRequestTypeException::new);

        return reportsRequest.execute(username, pageable);
    }

    public ReportResponse findReportById(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(ReportNotFoundException::new);

        return reportAssembler.of(report);
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(MemberNotFoundException::new);
    }

    @Transactional
    public void deleteReport(Long reportId, LoginMember loginMember) {
        Member member = findMemberById(loginMember.getId());
        Report report = reportRepository.findById(reportId)
            .orElseThrow(ReportNotFoundException::new);

        verifyIsAllowedUser(member, report);

        reportRepository.deleteById(reportId);
    }
}