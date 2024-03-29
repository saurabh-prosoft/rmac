<template>
  <section>
    <header>
      <Icon alt="Config icon" name="icons/config" adaptive :size="1.4" />
      <h2>Config</h2>
    </header>
    <div class="host-content">
      <div v-if="loading" class="config-spinner">
        <Spinner :size="2" />
      </div>
      <div class="no-config" v-else-if="!host.config || !host.health">
        <Icon alt="Offline icon" name="icons/host-offline" :size="2" adaptive />
        <h2>Host is offline</h2>
      </div>
      <div v-else class="config">
        <Property
          v-for="name in configProperties"
          :key="name"
          type="config"
          :id="host.id"
          :name="name"
          :input-type="configTypes[name]"
          editable
        >
          {{ capitalize(name) }}
        </Property>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted, computed, watch, capitalize } from 'vue';
import { useStore } from 'vuex';
import Icon from '../Common/Icon/Icon.vue';
import Property from '../Common/Property/Property.vue';
import Spinner from '../Common/Spinner/Spinner.vue';
import { configTypes } from '@/utils';
import bus from '@/event';
import { notifications } from '@/store/constants';

const store = useStore();

const props = defineProps({
  host: {
    type: Object,
    required: true,
  },
});

const loading = ref(false);

const configProperties = computed(() =>
  Object.keys(props.host.config).filter((name) => !'id, clientName, hostName'.includes(name))
);

onMounted(async () => {
  loading.value = true;
  await store.dispatch('fetchConfig', props.host.id);
  loading.value = false;
});

watch(
  () => props.host.health,
  async (newVal, oldVal) => {
    if (newVal && !oldVal) {
      bus.emit('notify', notifications.IHOST_ONLINE());
    }
    if (newVal && !props.host.config) {
      loading.value = true;
      await store.dispatch('fetchConfig', props.host.id);
      loading.value = false;
    }
  }
);
</script>

<style scoped>
.no-config,
.config-spinner {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}
.no-config:deep(.icon-container) {
  opacity: 0.6;
  font-size: 0;
  margin-right: 0.5rem;
}

.config {
  width: 100%;
  overflow-wrap: break-word;
}
</style>
