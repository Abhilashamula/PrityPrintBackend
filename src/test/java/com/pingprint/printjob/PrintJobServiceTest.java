package com.pingprint.printjob;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PrintJobServiceTest {
    @Test void returnsExistingJobInsteadOfCreatingDuplicatePhysicalJob() {
        PrintJobRepository repository = mock(PrintJobRepository.class);
        PrintOrder order = mock(PrintOrder.class); PrintJob existing = mock(PrintJob.class); UUID orderId = UUID.randomUUID();
        when(order.getId()).thenReturn(orderId); when(repository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
        assertThat(new PrintJobService(repository).enqueueOnce(order)).isSameAs(existing);
        verify(repository, never()).saveAndFlush(any());
    }
}
