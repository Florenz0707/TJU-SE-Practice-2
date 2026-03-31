package cn.edu.tju.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.business.model.bo.Business;
import cn.edu.tju.business.model.vo.BusinessSnapshotVO;
import cn.edu.tju.business.repository.BusinessRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessInternalServiceTest {

  @Mock private BusinessRepository businessRepository;

  @InjectMocks private BusinessInternalService businessInternalService;

  @Test
  void getBusinessSnapshotsFiltersDeletedBusinesses() {
    Business active = new Business();
    active.setId(1L);
    active.setBusinessName("active-business");
    active.setDeleted(false);

    Business deleted = new Business();
    deleted.setId(2L);
    deleted.setBusinessName("deleted-business");
    deleted.setDeleted(true);

    when(businessRepository.findAll()).thenReturn(List.of(active, deleted));

    List<BusinessSnapshotVO> snapshots = businessInternalService.getBusinessSnapshots();

    assertThat(snapshots).hasSize(1);
    assertThat(snapshots.getFirst().getId()).isEqualTo(1L);
    assertThat(snapshots.getFirst().getBusinessName()).isEqualTo("active-business");
  }

  @Test
  void getBusinessSnapshotByIdReturnsSnapshotWhenBusinessExists() {
    Business business = new Business();
    business.setId(10L);
    business.setBusinessName("merchant-10");

    when(businessRepository.findById(10L)).thenReturn(Optional.of(business));

    BusinessSnapshotVO snapshot = businessInternalService.getBusinessSnapshotById(10L);

    assertThat(snapshot).isNotNull();
    assertThat(snapshot.getId()).isEqualTo(10L);
    assertThat(snapshot.getBusinessName()).isEqualTo("merchant-10");
    verify(businessRepository).findById(10L);
  }

  @Test
  void getBusinessSnapshotByIdReturnsNullWhenBusinessIdIsNull() {
    BusinessSnapshotVO snapshot = businessInternalService.getBusinessSnapshotById(null);

    assertThat(snapshot).isNull();
  }
}