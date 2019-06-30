# VerifyFormScore
识别表格分数并验证结果

![](https://github.com/yuepengfeidev/VerifyFormScore/gif/gif1.gif)

一、表格分数识别使用两种方法：
1.TensorFlow识别，拥有60000手写数字的训练库，识别率较高，且稳定，只能一个一个字识别。
优点：容易训练字库，识别稳定，准确率高         
缺点：只能单个字识别
2.Tess-Two识别，可自行通过jTessBoxEditor训练，对于原图识别率也较高，可识别多位数字。
优点：可识别多个字     
缺点：当有杂点时，也会将其识别为数字，且不容易训练字库，识别不稳定

二、表格图像处理：
使用OpenCv，图像经过灰度化，二值化，腐蚀，膨胀，矫正等操作，截取到表格，接着通过腐蚀，膨胀处理得到表格横向分割线二值图和
竖向分割线二值图，得到各条表格分割线的坐标位置，计算出表格列数和行数，通过坐标分割表格，得到各个分数模块。

三、相机：
自定义Camera2，自定义选区View附在相机界面上，便于截取各个模块的图片。



