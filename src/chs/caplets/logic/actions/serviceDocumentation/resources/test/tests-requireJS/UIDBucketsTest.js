/**
 * Created with IntelliJ IDEA.
 * User: mukumar
 * Date: 10/10/12
 * Time: 11:50 AM
 * To change this template use File | Settings | File Templates.
 */
/*global $,  assertEquals, assertTrue*/
TestCase("UIDBucketsTest", {
    setUp: function ()
    {
        "use strict";
    },
    tearDown: function ()
    {
        "use strict";
    },
    "test UID Buckets should be the modulus of timestamp": function ()
    {
        "use strict";
        var objectUid = "uid1-64-host", bucket;
        bucket = mentor.publisher.uidBuckets.getUIDBucket(objectUid);
        assertEquals("Wrong bucket retrieved", "0", bucket);

        objectUid = "uid1-3-host";
        bucket = mentor.publisher.uidBuckets.getUIDBucket(objectUid);
        assertEquals("Wrong bucket retrieved", "3", bucket);

        objectUid = "uid1-71-host";
        bucket = mentor.publisher.uidBuckets.getUIDBucket(objectUid);
        assertEquals("Wrong bucket retrieved", "13", bucket);

    }
});
