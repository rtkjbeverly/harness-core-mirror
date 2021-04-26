package io.harness.ngtriggers.eventmapper.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.dto.TriggerMappingRequestData;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.webhook.CustomWebhookTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.eventmapper.filters.impl.AccountCustomTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.HeaderTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.JexlConditionsTriggerFilter;
import io.harness.ngtriggers.eventmapper.filters.impl.PayloadConditionsTriggerFilter;
import io.harness.ngtriggers.helpers.TriggerFilterStore;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class WebhookEventToTriggerMapperTest extends CategoryTest {
  private String ngTriggerCustomYaml;
  @InjectMocks @Inject private TriggerFilterStore triggerFilterStore;
  @Mock private NGTriggerService ngTriggerService;
  @Mock private NGTriggerElementMapper ngTriggerElementMapper;
  @InjectMocks @Inject AccountCustomTriggerFilter accountCustomTriggerFilter;
  @InjectMocks @Inject PayloadConditionsTriggerFilter payloadConditionsTriggerFilter;
  @InjectMocks @Inject HeaderTriggerFilter headerTriggerFilter;
  @InjectMocks @Inject JexlConditionsTriggerFilter jexlConditionsTriggerFilter;
  @InjectMocks @Inject CustomWebhookEventToTriggerMapper customWebhookEventToTriggerMapper;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ng-custom-trigger.yaml";
    ngTriggerCustomYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    FieldUtils.writeField(customWebhookEventToTriggerMapper, "triggerFilterStore", triggerFilterStore, true);
    FieldUtils.writeField(triggerFilterStore, "accountCustomTriggerFilter", accountCustomTriggerFilter, true);
    FieldUtils.writeField(triggerFilterStore, "payloadConditionsTriggerFilter", payloadConditionsTriggerFilter, true);
    FieldUtils.writeField(triggerFilterStore, "jexlConditionsTriggerFilter", jexlConditionsTriggerFilter, true);
    FieldUtils.writeField(triggerFilterStore, "headerTriggerFilter", headerTriggerFilter, true);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testMapCustomWebhookEventToTriggers() {
    TriggerWebhookEvent event =
        TriggerWebhookEvent.builder()
            .accountId("accountId")
            .createdAt(1l)
            .sourceRepoType("CUSTOM")
            .headers(Arrays.asList(
                HeaderConfig.builder().key("content-type").values(Arrays.asList("application/json")).build()))
            .build();
    NGTriggerEntity ngTriggerEntity = NGTriggerEntity.builder().yaml(ngTriggerCustomYaml).build();
    List<NGTriggerEntity> ngTriggerEntityList = new ArrayList<>();
    ngTriggerEntityList.add(ngTriggerEntity);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .source(NGTriggerSource.builder()
                        .spec(WebhookTriggerConfig.builder().spec(CustomWebhookTriggerSpec.builder().build()).build())
                        .build())
            .build();

    when(ngTriggerService.findTriggersForCustomWehbook(event, false, true)).thenReturn(ngTriggerEntityList);
    doReturn(ngTriggerConfig).when(ngTriggerElementMapper).toTriggerConfig(ngTriggerCustomYaml);
    WebhookEventMappingResponse webhookEventMappingResponse =
        customWebhookEventToTriggerMapper.mapWebhookEventToTriggers(
            TriggerMappingRequestData.builder().triggerWebhookEvent(event).build());
    assertThat(webhookEventMappingResponse).isNotNull();
    assertThat(webhookEventMappingResponse.getTriggers().size()).isEqualTo(1);
    assertThat(webhookEventMappingResponse.isCustomTrigger()).isTrue();
  }
}