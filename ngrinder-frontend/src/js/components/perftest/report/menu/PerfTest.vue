<template>
    <div class="detail-report-perftest-menu">
        <div class="pb-2 mt-4 mb-3 border-bottom">
            <h4 v-text="'Performance'"></h4>
            <button @click.prevent="downloadCSV" class="download-csv btn btn-primary float-right">
                <i class="fa fa-download mr-1"></i>
                <span v-text="i18n('perfTest.report.downloadCSV')"></span>
            </button>
        </div>

        <div v-show="tpsChart">
            <h6>
                <span v-text="'TPS'"></span>
                <span data-toggle="popover"
                      data-html="true"
                      data-trigger="hover"
                      :data-content="i18n('perfTest.report.tps.help')"
                      :title="i18n('perfTest.report.tps')">
                    <i class="fa fa-question-circle"></i>
                </span>
            </h6>
            <div class="bigchart" ref="tpsChart" id="tps-chart"></div>
        </div>

        <div v-show="meanTimeChart">
            <h6 v-text="`${i18n('perfTest.report.header.meantime')} (ms)`"></h6>
            <div class="chart" id="mean-time-chart"></div>
        </div>

        <div v-show="meanTimeToFirstByteChart">
            <h6 v-text="`${i18n('perfTest.report.header.meantimeToFirstByte')} (ms)`"></h6>
            <div class="chart" id="min-time-first-byte-chart"></div>
        </div>

        <div v-show="vuserChart">
            <h6 v-text="i18n('perfTest.report.header.vuser')"></h6>
            <div class="chart" id="vuser-chart"></div>
        </div>

        <div v-show="userDefinedData.length">
            <template v-for="(data, index) in userDefinedData">
                <h6 v-text="data.title"></h6>
                <div class="chart" :id="`user-defined-chart-${index}`"></div>
            </template>
        </div>

        <div v-show="errorChart">
            <h6 v-text="i18n('perfTest.report.header.errors')"></h6>
            <div class="chart" id="error-chart"></div>
        </div>
    </div>
</template>

<script>
    import { Mixins } from 'vue-mixin-decorator';
    import Component from 'vue-class-component';
    import Base from '../../../Base.vue';
    import MessagesMixin from '../../../common/mixin/MessagesMixin.vue';
    import ChartMixin from '../../../common/mixin/ChartMixin.vue';

    @Component({
        name: 'perfTest',
        props: {
            id: {
                type: [Number, String],
                required: true,
            },
        },
    })
    export default class PerfTest extends Mixins(Base, ChartMixin, MessagesMixin) {
        tpsChart = {};
        meanTimeChart = {};
        meanTimeToFirstByteChart = {};
        vuserChart = {};
        errorChart = {};
        userDefinedData = [];

        mounted() {
            this.$http.get(`/perftest/api/${this.id}/perf`, {
                params: {
                    dataType: 'TPS,Errors,Mean_Test_Time_(ms),Mean_time_to_first_byte,User_defined,Vuser',
                    imgWidth: parseInt(this.$refs.tpsChart.offsetWidth),
                },
            }).then(res => {
                const numOfTestRecord = Object.keys(res.data.TPS).length;
                const interval = res.data.chartInterval;
                const userDefinedData = res.data.User_defined || {};
                this.processUserDefinedData(userDefinedData, numOfTestRecord);

                this.tpsChart = this.drawChart('tps-chart', res.data.TPS, interval);
                this.meanTimeChart = this.drawChart('mean-time-chart', res.data.Mean_Test_Time_ms, interval);
                this.meanTimeToFirstByteChart = this.drawChart('min-time-first-byte-chart', res.data.Mean_time_to_first_byte, interval);
                this.vuserChart = this.drawChart('vuser-chart', res.data.Vuser, interval);
                this.errorChart = this.drawChart('error-chart', res.data.Errors, interval);
                this.$nextTick(() => {
                    this.userDefinedData.forEach((each, index) => this.drawChart(`user-defined-chart-${index}`, each.data, interval));
                });

            }).catch(() => this.showErrorMsg(this.i18n('common.message.loading.error')));

            $('[data-toggle="popover"]').popover();
        }

        processUserDefinedData(userDefinedData, numOfTestRecord) {
            let dataType;
            let userDefinedDataTemp;

            Object.entries(userDefinedData).forEach(([key, value], index) => {
                if (index % numOfTestRecord === 0) {
                    dataType = key;
                    userDefinedDataTemp = { title: this.getUserDefinedChartTitle(key), data: {}, };
                    userDefinedDataTemp.data['Total'] = value;
                    this.userDefinedData[index/numOfTestRecord] = userDefinedDataTemp;
                } else {
                    userDefinedDataTemp.data[this.getTestDesc(dataType, key)] = value;
                }
            });
        }

        getUserDefinedChartTitle(dataType) {
            const title = dataType.replace(/User_defined|_/g, ' ').trim();
            return title ? title : this.i18n('perfTest.report.header.userDefinedChart');
        }

        getTestDesc(dataType, dataFileName) {
            dataFileName = dataFileName.replace(dataType, '');
            return dataFileName.substring(dataFileName.indexOf('_') + 1)
                .replace(/_/g, ' ')
                .trim();
        }

        downloadCSV() {
            location.href = `${this.contextPath}/perftest/${this.id}/download_csv`;
        }
    }
</script>

<style lang="less" scoped>
    .detail-report-perftest-menu {

        .download-csv {
            margin-top: -36px;
        }

        .icon-question-sign {
            vertical-align: middle;
        }

        div {
            &.bigchart, &.chart {
                border: 1px solid #c4c4c4;
                height: 300px;
                min-width: 615px;
                margin-bottom: 20px;

                &.chart {
                    height: 200px;
                }
            }
        }
    }
</style>
