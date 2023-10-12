package com.company;

import com.company.Queue.Queue;

import java.util.List;

public class SRT extends AbstractBaseScheduler {


    private void addingNewProcessesToReadyQueue(int counter) {
        while (true) {
            if (!newQueue.isEmpty() && ((Process) newQueue.first()).getArrivalTime() == counter) {
                var process = (Process) newQueue.dequeue();

                process.setRemainingWorkTime(process.getBurstTime1());

                readyQueue.enqueue(process);
            } else
                break;
        }
    }

    private void stopProcessOrEnterIo(int counter) {
        if (!runningQueue.isEmpty()) {
            if (((Process) runningQueue.first()).getRemainingWorkTime() == 0 ) {
                var process = (Process) runningQueue.dequeue();

                switch (process.getRunningProcessSituation()) {
                    case CPU1 -> {
                        if (process.getIoTime() == 0){
                            process.setRunningProcessSituation(ProcessSituation.CPU2);
                            //process.setFinishTime(counter + process.getBurstTime2());
                            //????????????
                            process.setWaitingTime(process.getWaitingTime() + counter - process.getLastUsed());
                            runningQueue.enqueue(process);
                        } else {
                            process.setRunningProcessSituation(ProcessSituation.IO);
                            process.setFinishTime(counter + process.getIoTime());
                            process.setLastUsed(counter + process.getIoTime());
                            ioQueue.enqueue(process);
                        }
                        break;
                    }
                    case CPU2 -> {
                        process.setRunningProcessSituation(ProcessSituation.FINISHED);
                        process.setEndTime(counter);
                        finishedQueue.enqueue(process);
                        break;
                    }
                    default -> throw new IllegalArgumentException("process is not in not running in burst mode");
                }
            }
        }
    }

    private void checkIfIoIsFinished(int counter) {
        while (true) {
            if (!ioQueue.isEmpty() && ((Process) ioQueue.first()).getFinishTime() == counter) {
                var process = (Process) ioQueue.dequeue();

                if (process.getRunningProcessSituation().equals(ProcessSituation.IO)) {

                    process.setRemainingWorkTime(process.getBurstTime2());
                    readyQueue.enqueue(process);
                }
            } else
                break;
        }
    }

    private void forcedRemoveRunningProcess(int counter){
        if (readyQueue.size() > 1)
            Process.sortProcessByRemainingTime(readyQueue);

        if (!runningQueue.isEmpty() && !readyQueue.isEmpty()){
            if (((Process) runningQueue.first()).getRemainingWorkTime() > ((Process) readyQueue.first()).getRemainingWorkTime()){
                var process = (Process) runningQueue.dequeue();
                process.setLastUsed(counter);
                readyQueue.enqueue(process);
            }

        }

    }

    private void checkWhichCpuBurstTimeToRun(int counter) {
        // if SJF -> we should order the queue based on minimum remaining time for each process
        if (readyQueue.size() > 1)
            Process.sortProcessByRemainingTime(readyQueue);

        if (runningQueue.isEmpty() && !readyQueue.isEmpty()) {
            var process = (Process) readyQueue.dequeue();

            switch (process.getRunningProcessSituation()) {
                case FirstTime -> {
                    if (process.getBurstTime1() == 0){
                        process.setRunningProcessSituation(ProcessSituation.IO);
                        process.setStartTime(counter);
                        process.setFinishTime(counter + process.getIoTime());
                        process.setLastUsed(counter + process.getIoTime());
                        ioQueue.enqueue(process);
                    } else {
                        process.setRunningProcessSituation(ProcessSituation.CPU1);
                        process.setStartTime(counter);
                        //process.setFinishTime(counter + process.getBurstTime1());
                        process.setResponseTime(counter - process.getArrivalTime());
                        process.setWaitingTime(counter - process.getArrivalTime());
                        runningQueue.enqueue(process);
                    }
                    break;
                }

                case IO -> {
                    process.setRunningProcessSituation(ProcessSituation.CPU2);
                    //process.setFinishTime(counter + process.getBurstTime2());
                    process.setWaitingTime(process.getWaitingTime() + counter - process.getLastUsed());
                    runningQueue.enqueue(process);
                    break;
                }

                case CPU2, CPU1 -> {
                    process.setWaitingTime(process.getWaitingTime() + counter - process.getLastUsed());
                    runningQueue.enqueue(process);
                }

                default -> throw new IllegalArgumentException("process not in a situation to be in CPU burst 1 or 2");
            }
        } else if (!runningQueue.isEmpty()){
            ((Process) runningQueue.first()).setRemainingWorkTime(((Process) runningQueue.first()).getRemainingWorkTime() - 1);
        }
    }

    public Queue<Process> schedule(List<Process> processes, int counter) {
        Process.sortProcessByArrivalTime(processes);

        for (Process process : processes) {
            newQueue.enqueue(process);
        }

        while (true) {
            // 1. adding to ready queue
            addingNewProcessesToReadyQueue(counter);


            // 2. stopping process or entering IO
            stopProcessOrEnterIo(counter);

            // 3. checking if we need to put it on ready queue
            checkIfIoIsFinished(counter);

            // 3,5. check if running process must be removed
            forcedRemoveRunningProcess(counter);

            // 4. checking to run which CPU burst time
            checkWhichCpuBurstTimeToRun(counter);

            // 5. Double check for zero inputs in each iteration
            checkIfIoIsFinished(counter);
            stopProcessOrEnterIo(counter);

            // 6. check if we have a running process or not
            if (runningQueue.isEmpty())
                idleTime++;

            counter++;

            // check if all processes are finished
            if (isFinished(counter))
                break;
        }

        return finishedQueue;
    }

}
