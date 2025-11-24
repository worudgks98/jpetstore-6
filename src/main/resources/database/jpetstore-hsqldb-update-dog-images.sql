--
--    Copyright 2010-2025 the original author or authors.
--
--    Licensed under the Apache License, Version 2.0 (the "License");
--    you may not use this file except in compliance with the License.
--    You may obtain a copy of the License at
--
--       https://www.apache.org/licenses/LICENSE-2.0
--
--    Unless required by applicable law or agreed to in writing, software
--    distributed under the License is distributed on an "AS IS" BASIS,
--    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--    See the License for the specific language governing permissions and
--    limitations under the License.
--

-- Update dog product images to correct mappings
-- dog1 = Bulldog, dog2 = Chihuahua, dog3 = Dalmation, dog4 = Labrador Retriever, dog5 = Golden Retriever, dog6 = Poodle

UPDATE PRODUCT SET descn = '<image src="../images/dog1.gif">Friendly dog from England' WHERE productid = 'K9-BD-01';
UPDATE PRODUCT SET descn = '<image src="../images/dog6.gif">Cute dog from France' WHERE productid = 'K9-PO-02';
UPDATE PRODUCT SET descn = '<image src="../images/dog3.gif">Great dog for a Fire Station' WHERE productid = 'K9-DL-01';
UPDATE PRODUCT SET descn = '<image src="../images/dog5.gif">Great family dog' WHERE productid = 'K9-RT-01';
UPDATE PRODUCT SET descn = '<image src="../images/dog4.gif">Great hunting dog' WHERE productid = 'K9-RT-02';
UPDATE PRODUCT SET descn = '<image src="../images/dog2.gif">Great companion dog' WHERE productid = 'K9-CW-01';

