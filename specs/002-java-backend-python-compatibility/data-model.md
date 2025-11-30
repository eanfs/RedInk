# Data Model: Java后端Python版本兼容完善

## API接口
| 属性 | 类型 | 描述 |
|------|------|------|
| path | String | API路径 |
| method | String | 请求方法 |
| parameters | List<Parameter> | 请求参数 |
| responseFormat | String | 响应格式 |

## Parameter
| 属性 | 类型 | 描述 |
|------|------|------|
| name | String | 参数名称 |
| type | String | 参数类型 |
| required | boolean | 是否必填 |
| description | String | 参数描述 |

## ResponseStructure
| 属性 | 类型 | 描述 |
|------|------|------|
| success | boolean | 请求是否成功 |
| data | Object | 响应数据 |
| error | String | 错误信息（如果请求失败） |

## ErrorMessage
| 属性 | 类型 | 描述 |
|------|------|------|
| code | String | 错误码 |
| message | String | 错误信息 |
| details | String | 错误详情 |
