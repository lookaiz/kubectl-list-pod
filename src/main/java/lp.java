//#usr/bin/env jbang "$0" "$@" ; exit $?
#!/usr/bin/env jbang

//DEPS info.picocli:picocli:4.5.0
//DEPS info.picocli:picocli-codegen:4.5.0
//DEPS io.fabric8:kubernetes-client:4.13.0
//DEPS com.massisframework:j-text-utils:0.3.4

import dnl.utils.text.table.TextTable;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.utils.PodStatusUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

// FROM : https://dev.to/ikwattro/write-a-kubectl-plugin-in-java-with-jbang-and-fabric8-566

@Command(name = "lp", mixinStandardHelpOptions = true, version = "lp 0.1", description = "lp made with jbang")
public class lp implements Callable<Integer> {

    private enum PodInfoState { RUNNING, FAILING }

    // https://www.fileformat.info/info/unicode/char/search.htm?
    private static final String CHECK_MARK = "\u2705";
    private static final String FIRE = "\uD83D\uDD25";

//    @Parameters(index = "0", description = "The greeting to print", defaultValue = "World!")
//    private String greeting;

    public static void main(String... args) {
        int exitCode = new CommandLine(new lp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        // your business logic goes here...
        printTable(getPods());
        return 0;
//        System.out.println("Hello " + greeting);
//        return 0;
    }

    private static List<PodInfo> getPods() {
        KubernetesClient kc;
        try {
            kc = new DefaultKubernetesClient();
        } catch (Exception e) {
            throw new RuntimeException("Unable to create default Kubernetes client", e);
        }

        return kc.pods()
                .list()
                .getItems()
                .stream()
                .map(pod -> {
                    PodInfoState state = PodStatusUtil.isRunning(pod) ? PodInfoState.RUNNING : PodInfoState.FAILING;
                    String message = null;
                    if (!state.equals(PodInfoState.RUNNING)) {
                        message = PodStatusUtil.getContainerStatus(pod).get(0).getState().getWaiting().getMessage();
                    }

                    return new PodInfo(pod.getMetadata().getName(), state, message);
                })
                .collect(Collectors.toList());
    }

    static class PodInfo {

        private final String name;
        private final PodInfoState state;
        private final String message;

        public PodInfo(String name, PodInfoState state, String message) {
            this.name = name;
            this.state = state;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public PodInfoState getState() {
            return state;
        }

        public String getMessage() {
            return message;
        }
    }



    private static void printTable(List<PodInfo> list) {
        final Object[][] tableData = list.stream()
                .map(podInfo -> new Object[] {
                        podInfo.getState().equals(PodInfoState.RUNNING) ? CHECK_MARK : FIRE,
                        podInfo.getName(),
                        podInfo.getState(),
                        podInfo.getMessage()
                })
                .toArray(Object[][]::new);
        String[] columnNames = {"", "name", "state", "message"};
        new TextTable(columnNames, tableData).printTable();
    }
}
