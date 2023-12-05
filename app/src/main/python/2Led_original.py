
import cv2
import numpy as np
import matplotlib.pyplot as plt
from scipy.optimize import curve_fit
import time
from com.ledid.lediddection.model import Data

def process_frame(frame, heigth, width, thresholdDefault, Npixel):
    try:
        st = time.time()
        array2 = [[0]]
        img = np.frombuffer(frame, dtype=np.uint8).reshape(heigth, width, 4)
        img = cv2.rotate(img, cv2.ROTATE_90_COUNTERCLOCKWISE)
        frame = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY) #

        #    height = frame.shape[0]
        width = frame.shape[1]
        #     print("height, width", height, width)

        ret, frame0 = cv2.threshold(frame, 140, 255, cv2.THRESH_BINARY)
        contours, hierarchy = cv2.findContours(frame0, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

        print("len contours: ", len(contours))
        if len(contours) > 0 and len(contours)<=50:
            contours = contours
        if len(contours) > 50 and len(contours)<100:
            contours = contours[0:len(contours):3]
        if len(contours) >= 100 and len(contours)<150:
            contours = contours[0:len(contours):5]
        if len(contours) >= 150 and len(contours)<200:
            contours = contours[0:len(contours):7]
        if len(contours) >= 200 and len(contours)<300:
            contours = contours[0:len(contours):9]
        if len(contours) >= 300 and len(contours)<500:
            contours = contours[0:len(contours):10]
        if len(contours) >= 500 and len(contours) < 700:
            contours = contours[0:len(contours):15]
        if len(contours) >= 700 and len(contours) < 1000:
            contours = contours[0:len(contours):25]
        if len(contours) >= 1000 and len(contours) < 1300:
            contours = contours[0:len(contours):30]
        # print("lennnnnnn6",len(contours))
        if len(contours) >= 1300 and len(contours) < 1800:
            contours = contours[0:len(contours):40]
        if len(contours) == 0 or len(contours) > 10000:
            print("contour over")
        print("len contours sau khi loc: ", len(contours))
        mass_centres_x = []
        mass_centres_y = []
        top = []
        bot = []

        for i in range(0, len(contours)):
            M = cv2.moments(contours[i], 0)
            if M["m00"] != 0:
                mass_centres_x.append(int(M['m10']/M['m00']))
                mass_centres_y.append(int(M['m01']/M['m00']))
            else:
                mass_centres_x.append(int(0))
                mass_centres_y.append(int(0))

        for i in range(0, len(contours)):
            x,y,w,h = cv2.boundingRect(contours[i])
            cv2.rectangle(frame0,(x,y),(x+w,y+h),(255,0,0),2)
            top.append(int(y))
            bot.append(int(y+h))
        # if len(contours) == 0 or len(contours) > 50:
        #     pass
        else:
            if len(mass_centres_x) == 0:
                mass_centres_x = np.zeros(5, dtype=int)
            if len(mass_centres_y) == 0:
                mass_centres_y = np.zeros(5, dtype=int)
            if len(top) == 0:
                top = np.zeros(5, dtype=int)
            if len(bot) == 0:
                bot = np.zeros(5, dtype=int)

            a, b = check_roi_tu_arr(mass_centres_x,mass_centres_y, top, bot, Npixel)
            if a[0] == a[1] == a[2] == a[3] ==0 or abs(a[1]-a[3]>=480) or a[1] == 480 or a[3] == 480:
                a = [0,0,0,103]
            if b[0] == b[1] == b[2] == b[3] ==0 or abs(b[1]-b[3]>=480) :
                b = [0,0,0,103]
            print(f"roi1: {a}, roi2: {b}")

            text1 = 'RoI1'
            text2 = 'RoI2'
            x = width

            frame2 = ve_roi(img, text1, a, x)
            frame2 = ve_roi(img, text2, b, x)

            frame2 = img

            #-----------------------------------------------------------------------------
            # frame = img
            array = a
            array_0 = b

            # a0,b0,c0,d0 = hxla.xu_ly_anh(frame, array, Npixel)
            # a0_0,b0_0,c0_0,d0_0 = hxla.xu_ly_anh(frame, array_0, Npixel)
            a0,b0,c0,d0 = xu_ly_anh(frame, array, Npixel)
            a0_0,b0_0,c0_0,d0_0 = xu_ly_anh(frame, array_0, Npixel)

            values_y = c0
            values_y_0 = c0_0
            row = 100
            threshold_code = [0,1,1,1,0,0,1,0,0,1]
            input_var = 4

            a00, a1, a2, a3, a4, a5, a6, a7, a8, a9 = xu_ly_y(array2, values_y, row, threshold_code, input_var)
            a00_0, a1_0, a2_0, a3_0, a4_0, a5_0, a6_0, a7_0, a8_0, a9_0 = xu_ly_y(array2, values_y_0, row, threshold_code, input_var)


            #        print("so lan xuat hien:", a7)
            print("data led 1:", a8)
            print("data led 2:", a8_0)
            #        print("so hang lay duoc: ", a9)
            et = time.time()
            #
            elapsed_time = round(et - st, 2)
            frame2 = cv2.rotate(frame2, cv2.ROTATE_90_CLOCKWISE)
        retval, buffer = cv2.imencode('.bmp', frame2)
        byte_array = buffer.tobytes()
        return Data(byte_array,str(a8),elapsed_time,str(a8_0))
    except:
        return Data([],str(""),0.0,str(""))

def check_roi_tu_arr(str_x, Y, top, bot, Npixel):
    j = 1
    k = 0
    str_out1 = np.zeros((50, 4), dtype = int)
    str_out2 = np.zeros((50, 4), dtype = int)
    str_out3 = np.zeros((50, 4), dtype = int)
    str_out4 = np.zeros((50, 4), dtype = int)

    str_out5 = np.zeros((50, 4), dtype = int)
    str_out6 = np.zeros((50, 4), dtype = int)
    str_out7 = np.zeros((50, 4), dtype = int)
    str_out8 = np.zeros((50, 4), dtype = int)

    n = len(str_x)

    max1 = str_x[0]
    min1 = str_x[0]

    str_out1[0][0] = str_x[0]
    str_out1[0][1] = Y[0]
    str_out1[0][2] = top[0]
    str_out1[0][3] = bot[0]

    for i in range(1,n):
        if abs(str_x[i] - max1) <= 0.2 * max1 or abs(str_x[i] - min1) <= 0.2 * min1:
            str_out1[j][0] = str_x[i]
            str_out1[j][1] = Y[i]
            str_out1[j][2] = top[i]
            str_out1[j][3] = bot[i]
            j = j + 1;

            if str_x[i] > max1:
                max1 = str_x[i]
            if str_x[i] < min1:
                min1 = str_x[i]

        else:
            str_out2[k][0] = str_x[i]
            str_out2[k][1] = Y[i]
            str_out2[k][2] = top[i]
            str_out2[k][3] = bot[i]
            k = k + 1

    max2 = str_out2[0][0]
    min2 = str_out2[0][0]
    str_out3[0][0] = str_out2[0][0]
    str_out3[0][1] = str_out2[0][1]
    str_out3[0][2] = str_out2[0][2]
    str_out3[0][3] = str_out2[0][3]

    j2 = 1
    k2 = 0

    for i2 in range(1, np.size(str_out2,0)):
        if abs(str_out2[i2][0] - max2) <= 0.2 * max2 or abs(str_out2[i2][0] - min2) <= 0.2 * min2:
            str_out3[j2][0] = str_out2[i2][0]
            str_out3[j2][1] = str_out2[i2][1]
            str_out3[j2][2] = str_out2[i2][2]
            str_out3[j2][3] = str_out2[i2][3]
            j2 = j2 + 1
            if str_out2[i2][0] > max2:
                max2 = str_out2[i2][0]
            if str_out2[i2][0] < min2:
                min2 = str_out2[i2][0]
        else:
            str_out4[k2][0] = str_out2[i2][0]
            str_out4[k2][1] = str_out2[i2][1]
            str_out4[k2][2] = str_out2[i2][2]
            str_out4[k2][3] = str_out2[i2][3]
            k2 = k2 + 1

    max3 = str_out4[0][0]
    min3 = str_out4[0][0]
    str_out5[0][0] = str_out4[0][0]
    str_out5[0][1] = str_out4[0][1]
    str_out5[0][2] = str_out4[0][2]
    str_out5[0][3] = str_out4[0][3]

    j3 = 1
    k3 = 0

    for i3 in range(1, np.size(str_out4,0)):
        if abs(str_out4[i3][0] - max3) <= 0.2 * max3 or abs(str_out4[i3][0] - min3) <= 0.2 * min3:
            str_out5[j3][0] = str_out4[i3][0]
            str_out5[j3][1] = str_out4[i3][1]
            str_out5[j3][2] = str_out4[i3][2]
            str_out5[j3][3] = str_out4[i3][3]
            j3 = j3 + 1
            if str_out4[i3][0] > max3:
                max3 = str_out4[i3][0]
            if str_out4[i3][0] < min3:
                min3 = str_out4[i3][0]
        else:
            str_out6[k3][0] = str_out4[i3][0]
            str_out6[k3][1] = str_out4[i3][1]
            str_out6[k3][2] = str_out4[i3][2]
            str_out6[k3][3] = str_out4[i3][3]
            k3 = k3 + 1

    max4 = str_out6[0][0]
    min4 = str_out6[0][0]
    str_out7[0][0] = str_out6[0][0]
    str_out7[0][1] = str_out6[0][1]
    str_out7[0][2] = str_out6[0][2]
    str_out7[0][3] = str_out6[0][3]

    j4 = 1
    k4 = 0

    for i4 in range(1, np.size(str_out6,0)):
        if abs(str_out6[i4][0] - max4) <= 0.2 * max4 or abs(str_out6[i4][0] - min4) <= 0.2 * min4:
            str_out7[j4][0] = str_out6[i4][0]
            str_out7[j4][1] = str_out6[i4][1]
            str_out7[j4][2] = str_out6[i4][2]
            str_out7[j4][3] = str_out6[i4][3]
            j4 = j4 + 1
            if str_out6[i4][0] > max4:
                max4 = str_out6[i4][0]
            if str_out6[i4][0] < min4:
                min4 = str_out6[i4][0]
        else:
            str_out8[k4][0] = str_out6[i4][0]
            str_out8[k4][1] = str_out6[i4][1]
            str_out8[k4][2] = str_out6[i4][2]
            str_out8[k4][3] = str_out6[i4][3]
            k4 = k4 + 1


    in1 = str_out1
    in2 = str_out3
    in3 = str_out5
    in4 = str_out7
    str_ = np.zeros(4, dtype = int)
    str_2 = np.zeros(4, dtype = int)
    str_outout1 = np.zeros(4, dtype = int)
    str_outout2 = np.zeros(4, dtype = int)



    n = 50
    i1 = i2 = i3 = i4 = 0
    j1 = j2 = j3 = j4 = 0
    tg = 0
    a = b = c = 0
    max1 = max2 = max3 = max4 = 0
    x_top = x_bot = 0
    x_top2 = x_bot2 = 0
    min1 = min2 = min3 = min4 = 0


    for i1 in range(0, 50):
        if in1[i1][0] != 0 and in1[i1][1] != 0:
            j1 = j1 + 1
        if in2[i1][0] != 0 and in2[i1][1] != 0:
            j2 = j2 + 1
        if in3[i1][0] != 0 and in3[i1][1] != 0:
            j3 = j3 + 1
        if in4[i1][0] != 0 and in4[i1][1] != 0:
            j4 = j4 + 1
    str_[0] = j1;
    str_[1] = j2;
    str_[2] = j3;
    str_[3] = j4;

    for a in range(0, 3):
        for b in range(a+1, 4):
            if str_[a] < str_[b]:
                tg = str_[a]
                str_[a] = str_[b]
                str_[b] = tg

    # truong hop co 1 den
    if str_[0] == j1:
        max1 = 0
        min1 = in1[0][3]
        for i2 in range(0, j1):
            if in1[i2][2] > max1:
                max1 = in1[i2][2]
                x_top = in1[i2][0]
            if in1[i2][3] <= min1:
                min1 = in1[i2][3]
                x_bot = in1[i2][0]
        str_outout1[0] = (x_top + x_bot)/2
        str_outout1[1] = min1
        str_outout1[2] = (x_top + x_bot)/2
        str_outout1[3] = max1

    if str_[0] == j2:
        max1 = 0
        min1 = in2[0][3]
        for i2 in range(0, j2):
            if in2[i2][2] > max1:
                max1 = in2[i2][2]
                x_top = in2[i2][0]
            if in2[i2][3] <= min1:
                min1 = in2[i2][3]
                x_bot = in2[i2][0]
        str_outout1[0] = (x_top + x_bot)/2
        str_outout1[1] = min1
        str_outout1[2] = (x_top + x_bot)/2
        str_outout1[3] = max1

    if str_[0] == j3:
        max1 = 0
        min1 = in3[0][3]
        for i2 in range(0, j3):
            if in3[i2][2] > max1:
                max1 = in3[i2][2]
                x_top = in3[i2][0]
            if in3[i2][3] <= min1:
                min1 = in3[i2][3]
                x_bot = in3[i2][0]
        str_outout1[0] = (x_top + x_bot)/2
        str_outout1[1] = min1
        str_outout1[2] = (x_top + x_bot)/2
        str_outout1[3] = max1

    if str_[0] == j4:
        max1 = 0
        min1 = in4[0][3]
        for i2 in range(0, j4):
            if in4[i2][2] > max1:
                max1 = in4[i2][2]
                x_top = in4[i2][0]
            if in4[i2][3] <= min1:
                min1 = in4[i2][3]
                x_bot = in4[i2][0]
        str_outout1[0] = (x_top + x_bot)/2
        str_outout1[1] = min1
        str_outout1[2] = (x_top + x_bot)/2
        str_outout1[3] = max1

    # truong hop co den so 2
    if abs(max1 - min1) >= 50*Npixel:
        str_outout2[0] = (x_top + x_bot)/2
        str_outout2[1] = max1 - 24*Npixel
        str_outout2[2] = (x_top + x_bot)/2
        str_outout2[3] = max1

        str_outout1[0] = (x_top + x_bot)/2
        str_outout1[1] = min1
        str_outout1[2] = (x_top + x_bot)/2
        str_outout1[3] = min1 + 28*Npixel
    else:
        if str_[1] == j1:
            max2 = 0
            min2 = in1[0][3]
            for i3 in range(0, j1):
                if in1[i3][2] > max2:
                    max2 = in1[i3][2]
                    x_top2 = in1[i3][0]
                if in1[i3][3] <= min2:
                    min2 = in1[i3][3]
                    x_bot2 = in1[i3][0]
            str_outout2[0] = (x_top2 + x_bot2)/2
            str_outout2[1] = min2
            str_outout2[2] = (x_top2 + x_bot2)/2
            str_outout2[3] = max2

        if str_[1] == j2:
            max2 = 0
            min2 = in2[0][3]
            for i3 in range(0, j2):
                if in2[i3][2] > max2:
                    max2 = in2[i3][2]
                    x_top2 = in2[i3][0]
                if in2[i3][3] <= min2:
                    min2 = in2[i3][3]
                    x_bot2 = in2[i3][0]
            str_outout2[0] = (x_top2 + x_bot2)/2
            str_outout2[1] = min2
            str_outout2[2] = (x_top2 + x_bot2)/2
            str_outout2[3] = max2

        if str_[1] == j3:
            max2 = 0
            min2 = in3[0][3]
            for i3 in range(0, j3):
                if in3[i3][2] > max2:
                    max2 = in3[i3][2]
                    x_top2 = in3[i3][0]
                if in3[i3][3] <= min2:
                    min2 = in3[i3][3]
                    x_bot2 = in3[i3][0]
            str_outout2[0] = (x_top2 + x_bot2)/2
            str_outout2[1] = min2
            str_outout2[2] = (x_top2 + x_bot2)/2
            str_outout2[3] = max2

        if str_[1] == j4:
            max2 = 0
            min2 = in4[0][3]
            for i3 in range(0, j2):
                if in4[i3][2] > max2:
                    max2 = in4[i3][2]
                    x_top2 = in4[i3][0]
                if in4[i3][3] <= min2:
                    min2 = in4[i3][3]
                    x_bot2 = in4[i3][0]
            str_outout2[0] = (x_top2 + x_bot2)/2
            str_outout2[1] = min2
            str_outout2[2] = (x_top2 + x_bot2)/2
            str_outout2[3] = max2

    return str_outout1, str_outout2


def xu_ly_anh(img, array, Npixel):
    x1 = array[0] #ok fix 480
    y1 = array[1]
    x2 = array[2]
    y2 = array[3]

    i=0
    Pixels_Line = [] # sua duoc loi pixel line la bien public


    if y2 - y1 <0:
        Pixels_Line = np.zeros((y1-y2+1),dtype = int)
        for y in range(y1, y2-1,-1):
            Pixels_Line[i] = img[y,x1]
            i = i + 1

    if y2 - y1 >0:
        Pixels_Line = np.zeros((y2-y1+1),dtype = int)
        for y in range(y1, y2+1,+1):
            Pixels_Line[i] = img[y,x1] # sua dung pixel 480 and 50
            i = i + 1

    N = len(Pixels_Line)

    # for i in range(0,N):
    #     Mang_input_waveform[i] = i/Npixel
    print(f"len(Pixel_Line): {N}")
    array_final = np.zeros(abs(y1-y2)+10, dtype = int)
    i = j = k = 0
    for k in range(0, N):
        array_final[k] = -1
    for i in range(0, N):
        if i%Npixel == 0:
            array_final[j] = Pixels_Line[i]
            j = j + 1
    # npixel_final = int(N/Npixel + 1)
    # return N, Pixels_Line, array_final, N, Mang_input_waveform, x_list
    return N, Pixels_Line, array_final, N

def ve_roi(img, text, array, x):
    x1 = array[0]
    x2 = array[1]
    x3 = array[2]
    x4 = array[3]

    y1 = 0
    y2 = x4

    if x3 + 10 > x:
        y1 = x3 - 15
    else:
        y1 = 5 + x3

    img = cv2.line(img,(x1,x2),(x3,x4),(0,0,255),1,cv2.LINE_AA)

    font = cv2.FONT_HERSHEY_SIMPLEX
    img = cv2.putText(img, text, (y1,y2), font, 1.5, (0,0,255),2,cv2.LINE_AA)
    return img

def xu_ly_y(array2, values_y, row, threshold_code, input_var):
    values_x = [int(i) for i in range(len(values_y))]
    mse_y_values = [int(i) for i in range(len(values_y))]

    def mapping1(values_x, a0, a1, a2, a3):
        return a3 * values_x**3 + a2 * values_x**2 + a1 * values_x + a0

    args, _ = curve_fit(mapping1, values_x, values_y)

    mse_final = 0
    for i in range(0,len(values_y)):
        mse_y = args[0] + args[1]*i + args[2]*i**2 + args[3] * i**3
        mse = ((values_y[i] - mse_y)**2)/len(values_y)
        mse_final += mse
        mse_y_values[i] = mse_y

    mang_so_sanh = [int(i) for i in range(len(values_y))]
    so_sanh = 0
    for i in range(0,len(values_y)):
        if values_y[i] >= mse_y_values[i]:
            so_sanh = 1
        else:
            so_sanh = 0
        mang_so_sanh[i] = so_sanh

    if len(mang_so_sanh) >= len(array2[0]):
        c = np.pad(array2, [(0, 0),(0, len(mang_so_sanh) - len(array2[0]))], mode='constant')
        array_append = np.append([mang_so_sanh],c,axis=0)
    else:
        c = np.pad(mang_so_sanh, (0, len(array2[0]) - len(mang_so_sanh)), 'constant')
        array_append = np.append([c],array2,axis=0)

    so_hang_lay_duoc = len(array_append[0])
    if so_hang_lay_duoc <= row:
        subarray = array_append
    else:
        subarray = array_append[0:row]

    a_man = []

    for i in range(0, len(threshold_code)):
        if threshold_code[i] == 1:
            a_man = np.append(a_man, [0,1])
        else:
            a_man = np.append(a_man, [1,0])
    a_man = list(map(int, a_man))
    so_mau = len(subarray)*len(subarray[0])

    n = len(values_y)
    s = 0
    for i in range(n):
        s = s + values_y[i]
    threshold = s/n

    mang_2d_dau_vao = subarray
    if np.size(mang_2d_dau_vao,0) < row:
        n_loop = np.size(mang_2d_dau_vao,0)
    else:
        n_loop = row
    c = np.size(mang_2d_dau_vao,1)
    d = np.size(threshold_code)
    b = threshold_code
    MANG = np.zeros((n_loop,20), dtype = int)
    MANG_index = np.zeros((n_loop,20), dtype = int)
    MANG_test = np.zeros((n_loop,20), dtype = int)
    MANG_heso = np.zeros((n_loop,20), dtype = int)
    MANG_daura = np.zeros(n_loop, dtype = int)

    i = j = k = m = n = o = 0
    x = np.zeros(20, dtype = int)
    heso = np.zeros(20, dtype = int)
    test = np.zeros(20, dtype = int)
    index = np.zeros(20, dtype = int)
    kiemtra = np.zeros(20, dtype = int)
    count = countdem = max1 = 0
    sizeb = 20
    max1_3 = 0
    value = -1
    size = sizeb

    for i_loop in range(0, n_loop):
        a = mang_2d_dau_vao[i_loop]
        i = 0
        n = 0
        for k in range(0,20):
            x[k] = -1
        for j in range(0, c-d+1):
            for m in range(0, d-input_var):
                n = n + abs(a[j+m] - b[m])
            if n == 0:
                for o in range(0, input_var):
                    # print("i,j,d,o, trong1, trong2", i,j,d,o, i+o, j+d-input_var+o)
                    if i + o < 20:  #ok fix xong 20. ctr chạy êm ru.
                        x[i + o] = a[j+d-input_var+o]
                    else:
                        pass

                i = i + input_var
            n = 0

        MANG[i_loop] = x
        for k2 in range(0, sizeb):
            heso[k2] = -1
            kiemtra[k2] = -1
        for i2 in range(0, sizeb - input_var, 2):
            if x[i2] == -1:
                break
            else:
                heso[int(i2/2)] = 0
                for j2 in range (0, input_var):
                    heso[int(i2/2)] = heso[int(i2/2)] + x[i2+j2]*(1 << j2)
                    kiemtra[i2] = 1 << j2

        print(f"heso sau khi dich bit:{heso[:]}")
        for l2 in range(0, 20):
            test[l2] = -1
            index[l2] = -1
        max1 = 0
        countdem = 0
        for m2 in range(0,20):
            count = 0
            countdem = 0
            for n2 in range(0, 20):
                if heso[m2] == test[n2]:
                    count = 1
            if count != 1:
                for o2 in range(0, 20):
                    if heso[m2] == heso[o2]:
                        countdem = countdem + 1
                index[max1] = countdem
                test[max1] = heso[m2]
                max1 = max1 + 1

        MANG_index[i_loop] = index
        MANG_test[i_loop]  = test
        MANG_heso[i_loop]  = heso

        max1_3 = 0
        value3 = -1
        for i3 in range(0, size):
            if index[i3] > max1_3:
                max1_3 = index[i3]
                value3 = test[i3]
        daura = value3
        MANG_daura[i_loop] = daura

    array = [input_var]*n_loop
    size4 = np.size(MANG_daura)

    test4 = np.zeros(20, dtype = int)
    index4 = np.zeros(20, dtype = int)

    count4 = countdem4 = max1_4 = max2_4 = daura4 = value4 = 0
    max3_4 = maxfinal = index3_4 = indexfinal = 0

    for l4 in range(0,20):
        test4[l4] = -1
        index4[l4] = -1

    max1_4 = 0
    countdem4 = 0
    size4 = len(MANG_daura)
    for m4 in range(0, size4):
        count4 = 0
        countdem4 = 0
        for n4 in range(0, 20):
            if MANG_daura[m4] == test4[n4] or MANG_daura[m4] == -1:
                count4 = 1
        if count4 != 1:
            for o4 in range(0,size4):
                if MANG_daura[m4] == MANG_daura[o4]:
                    countdem4 = countdem4 + 1
            index4[max1_4] = countdem4
            test4[max1_4] = MANG_daura[m4]
            max1_4 = max1_4 + 1
    max2_4 = 0
    value4 = -1
    for i4 in range(0, 20):
        if index4[i4] > max2_4:
            max3_4 = max2_4
            index3_4 = value4
            max2_4 = index4[i4]
            value4 = test4[i4]
    if value4 != -1:
        daura4 = 100*max2_4/size4
        indexfinal = value4
        maxfinal = max2_4
    else:
        daura4 = 100*max3_4/size4
        indexfinal = index3_4
        maxfinal = max3_4


    digit =  4
    def twosCom_decBin(dec, digit):
        bin1 = ""
        if dec>=0:
            bin1 = bin(dec).split("0b")[1]
            while len(bin1)<digit :
                bin1 = '0'+bin1
            return bin1
        else:
            bin1 = -1*dec
            return bin(bin1-pow(2,digit)).split("0b")[1]
    bin1 = twosCom_decBin(value4, 32)
    bin1 = str(bin1)[::-1]
    bin1 = [int(i) for i in str(bin1)]
    bin1 = bin1[0:digit]
    return threshold, so_mau, subarray, array_append, values_x, mang_so_sanh, daura4, maxfinal, bin1, so_hang_lay_duoc



