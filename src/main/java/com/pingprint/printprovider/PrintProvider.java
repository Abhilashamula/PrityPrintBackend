package com.pingprint.printprovider;

import com.pingprint.printjob.PrintJob;

public interface PrintProvider {
    String providerName();
    void submit(PrintJob job);
    void refreshStatus(PrintJob job);
}
