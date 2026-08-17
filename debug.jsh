import com.pasystem.client.SpeakerMgr;
import com.pasystem.client.MicMgr;
import com.pasystem.client.NetworkMgr;

System.out.println("=== PA System Debug ===");
System.out.println();
System.out.println("Connected Speakers:");
SpeakerMgr.get_speakers().forEach(speaker -> System.out.println("  " + speaker));

System.out.println();
System.out.println("Connected Microphones:");
MicMgr.get_mics().forEach(mic -> System.out.println("  " + mic));

System.out.println();
System.out.println("Valid Network Interfaces:");
NetworkMgr.get_all_valid_interfaces().forEach(iface -> 
    System.out.println("  DisplayName: " + iface.iface.getDisplayName() + " | IP: " + iface.ip + " | Subnet: " + iface.subnet)
);

System.out.println();
System.out.println("=== End Debug ===");

/exit
