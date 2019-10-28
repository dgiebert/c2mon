package cern.c2mon.cache.actions.process;

import cern.c2mon.cache.SupervisedServiceTest;
import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.server.common.process.Process;
import cern.c2mon.server.common.process.ProcessCacheObject;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

public class ProcessSupervisedServiceTest extends SupervisedServiceTest<Process> {

  @Autowired
  private C2monCache<Process> equipmentCacheRef;

  @Autowired
  @Getter
  private ProcessService supervisedService;

  @Override
  protected Process getSample() {
    return new ProcessCacheObject(1L);
  }

  @Override
  protected C2monCache<Process> initCache() {
    return equipmentCacheRef;
  }
}
